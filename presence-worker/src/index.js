const AUTH_TIMEOUT_MS = 30_000;
const MIN_REFRESH_INTERVAL_MS = 10_000;
const MAX_MESSAGE_LENGTH = 2_048;
const MIN_CHAT_INTERVAL_MS = 1_500;
const MAX_CHAT_LENGTH = 256;
const MAX_MUTE_SECONDS = 7 * 24 * 60 * 60;
// Lynaah - the only account allowed to mute/unmute Runal Chat. This is enforced here, not
// client-side, since the client is open source and a client-side-only check would be
// trivial to bypass; this UUID comes from the same Mojang session-join handshake already
// used to authenticate presence, so it can't be spoofed.
const ADMIN_UUID = "395e68bdb06e40fdbcdfbc7c146dcf15";

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store"
    }
  });
}

function randomServerId() {
  const bytes = new Uint8Array(20);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, byte => byte.toString(16).padStart(2, "0")).join("");
}

function normalizeUuid(value) {
  return String(value || "").replaceAll("-", "").toLowerCase();
}

function isValidName(value) {
  return typeof value === "string" && /^[A-Za-z0-9_]{1,16}$/.test(value);
}

function isValidUuid(value) {
  return /^[0-9a-f]{32}$/.test(normalizeUuid(value));
}

export class PresenceRoom {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
  }

  async fetch(request) {
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return json({ error: "websocket upgrade required" }, 426);
    }

    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    const serverId = randomServerId();

    this.ctx.acceptWebSocket(server);
    server.serializeAttachment({
      authenticated: false,
      serverId,
      connectedAt: Date.now()
    });
    server.send(JSON.stringify({ action: "auth_request", serverId }));

    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(socket, rawMessage) {
    const text = typeof rawMessage === "string"
      ? rawMessage
      : new TextDecoder().decode(rawMessage);

    if (text.length > MAX_MESSAGE_LENGTH) {
      socket.close(1009, "message too large");
      return;
    }

    let message;
    try {
      message = JSON.parse(text);
    } catch {
      socket.close(1003, "invalid json");
      return;
    }

    const attachment = socket.deserializeAttachment() || {};
    const isRefreshResponse = attachment.authenticated
      && message.action === "auth_response"
      && attachment.pendingServerId;

    if (attachment.authenticated && !isRefreshResponse) {
      if (message.action === "ping") {
        socket.send(JSON.stringify({ action: "pong" }));
      } else if (message.action === "refresh") {
        const now = Date.now();
        const lastRefreshAt = Number(
          attachment.lastRefreshAt || attachment.connectedAt || 0
        );
        if (now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) return;

        const pendingServerId = randomServerId();
        socket.serializeAttachment({
          ...attachment,
          pendingServerId,
          refreshStartedAt: now,
          lastRefreshAt: now
        });
        socket.send(JSON.stringify({
          action: "auth_request",
          serverId: pendingServerId
        }));
      } else if (message.action === "chat") {
        await this.handleChat(socket, attachment, message);
      } else if (message.action === "mute") {
        await this.handleMuteToggle(socket, attachment, message, true);
      } else if (message.action === "unmute") {
        await this.handleMuteToggle(socket, attachment, message, false);
      }
      return;
    }

    if (message.action !== "auth_response") {
      console.warn(`Closing socket: expected auth_response, got action=${message.action}`);
      socket.close(1008, "authentication required");
      return;
    }

    const challengeStartedAt = isRefreshResponse
      ? attachment.refreshStartedAt
      : attachment.connectedAt;
    if (Date.now() - Number(challengeStartedAt || 0) > AUTH_TIMEOUT_MS) {
      console.warn("Closing socket: authentication expired before auth_response arrived");
      socket.close(1008, "authentication expired");
      return;
    }

    const name = message.name;
    const uuid = normalizeUuid(message.uuid);
    const serverId = isRefreshResponse
      ? attachment.pendingServerId
      : attachment.serverId;
    const identityChanged = isRefreshResponse
      && (uuid !== attachment.uuid || name !== attachment.name);
    if (!isValidName(name) || !isValidUuid(uuid) || !serverId || identityChanged) {
      console.warn(`Closing socket: invalid identity name=${JSON.stringify(name)} uuid=${JSON.stringify(message.uuid)} serverId=${serverId}`);
      socket.close(1008, "invalid identity");
      return;
    }

    console.log(`${isRefreshResponse ? "Refreshed" : "Accepted"} presence claim for ${name} (${uuid})`);

    socket.serializeAttachment({
      authenticated: true,
      uuid,
      name,
      serverId,
      version: String(message.version || "unknown").slice(0, 32),
      connectedAt: attachment.connectedAt,
      lastRefreshAt: Date.now()
    });
    socket.send(JSON.stringify({ action: "auth_success" }));
    this.broadcastSnapshot();
  }

  async getMutes() {
    return (await this.ctx.storage.get("mutedUsers")) || {};
  }

  async setMutes(mutes) {
    await this.ctx.storage.put("mutedUsers", mutes);
  }

  async handleChat(socket, attachment, message) {
    const now = Date.now();
    const lastChatAt = Number(attachment.lastChatAt || 0);
    if (now - lastChatAt < MIN_CHAT_INTERVAL_MS) return;

    const text = String(message.text || "").slice(0, MAX_CHAT_LENGTH).trim();
    if (!text) return;

    const server = String(message.server || "unknown").slice(0, 32);

    const mutes = await this.getMutes();
    const expiresAt = mutes[attachment.name.toLowerCase()];
    if (expiresAt && expiresAt > now) {
      socket.send(JSON.stringify({ action: "chat_error", reason: "muted" }));
      return;
    }

    socket.serializeAttachment({ ...attachment, lastChatAt: now });

    const payload = JSON.stringify({ action: "chat", name: attachment.name, server, text });
    for (const peer of this.ctx.getWebSockets()) {
      const peerAttachment = peer.deserializeAttachment() || {};
      if (!peerAttachment.authenticated) continue;
      try {
        peer.send(payload);
      } catch {
        // The close/error callback will refresh the remaining clients.
      }
    }
  }

  async handleMuteToggle(socket, attachment, message, muting) {
    if (attachment.uuid !== ADMIN_UUID) {
      socket.send(JSON.stringify({ action: "chat_error", reason: "not_admin" }));
      return;
    }
    if (!isValidName(message.target)) return;

    const target = message.target.toLowerCase();
    const mutes = await this.getMutes();
    if (muting) {
      const seconds = Math.max(1, Math.min(MAX_MUTE_SECONDS, Number(message.seconds) || 600));
      mutes[target] = Date.now() + seconds * 1000;
    } else {
      delete mutes[target];
    }
    await this.setMutes(mutes);
    socket.send(JSON.stringify({ action: "mute_ack", target: message.target, muting }));
  }

  webSocketClose() {
    this.broadcastSnapshot();
  }

  webSocketError() {
    this.broadcastSnapshot();
  }

  broadcastSnapshot() {
    const usersByUuid = new Map();
    const sockets = this.ctx.getWebSockets();

    for (const socket of sockets) {
      const attachment = socket.deserializeAttachment() || {};
      if (!attachment.authenticated || !attachment.uuid) continue;
      usersByUuid.set(attachment.uuid, {
        uuid: attachment.uuid,
        name: attachment.name,
        serverId: attachment.serverId
      });
    }

    const payload = JSON.stringify({
      action: "sync",
      users: Array.from(usersByUuid.values())
    });

    for (const socket of sockets) {
      const attachment = socket.deserializeAttachment() || {};
      if (!attachment.authenticated) continue;
      try {
        socket.send(payload);
      } catch {
        // The close/error callback will refresh the remaining clients.
      }
    }
  }
}

async function handleLatestVersion() {
  try {
    // cacheTtl caches the upstream GitHub response at Cloudflare's edge - without it,
    // every player join would hit GitHub's API directly, and unauthenticated GitHub API
    // calls are capped at 60/hour per source IP (all of which come from Cloudflare here).
    const response = await fetch("https://api.github.com/repos/lunahpvp/runal-mod/releases/latest", {
      headers: {
        "User-Agent": "runal-presence-worker",
        "Accept": "application/vnd.github+json"
      },
      cf: { cacheTtl: 600, cacheEverything: true }
    });
    if (!response.ok) return json({ error: "upstream error" }, 502);

    const data = await response.json();
    const version = String(data.tag_name || "").replace(/^v/, "");
    if (!version) return json({ error: "no version" }, 502);

    return json({ version });
  } catch {
    return json({ error: "fetch failed" }, 502);
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/" || url.pathname === "/health") {
      return json({ service: "runal-presence", status: "ok" });
    }
    if (url.pathname === "/latest-version") {
      return handleLatestVersion();
    }
    if (url.pathname !== "/presence") {
      return json({ error: "not found" }, 404);
    }

    const room = env.PRESENCE.getByName("global");
    return room.fetch(request);
  }
};
