const SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
const AUTH_TIMEOUT_MS = 30_000;
const MAX_MESSAGE_LENGTH = 2_048;

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
    if (attachment.authenticated) {
      if (message.action === "ping") {
        socket.send(JSON.stringify({ action: "pong" }));
      }
      return;
    }

    if (message.action !== "auth_response") {
      socket.close(1008, "authentication required");
      return;
    }

    if (Date.now() - Number(attachment.connectedAt || 0) > AUTH_TIMEOUT_MS) {
      socket.close(1008, "authentication expired");
      return;
    }

    const name = message.name;
    const uuid = normalizeUuid(message.uuid);
    if (!isValidName(name) || !isValidUuid(uuid) || !attachment.serverId) {
      socket.close(1008, "invalid identity");
      return;
    }

    const authenticatedProfile = await this.verifyMinecraftSession(
      name,
      uuid,
      attachment.serverId
    );
    if (!authenticatedProfile) {
      socket.close(1008, "minecraft authentication failed");
      return;
    }

    socket.serializeAttachment({
      authenticated: true,
      uuid,
      name: authenticatedProfile.name,
      version: String(message.version || "unknown").slice(0, 32),
      connectedAt: attachment.connectedAt
    });
    socket.send(JSON.stringify({ action: "auth_success" }));
    this.broadcastSnapshot();
  }

  async verifyMinecraftSession(name, uuid, serverId) {
    const url = new URL(SESSION_SERVER_URL);
    url.searchParams.set("username", name);
    url.searchParams.set("serverId", serverId);

    let response;
    try {
      response = await fetch(url, {
        headers: {
          "accept": "application/json",
          "user-agent": "Runal-Presence/1.0"
        }
      });
    } catch {
      return null;
    }

    if (!response.ok) return null;

    try {
      const profile = await response.json();
      if (normalizeUuid(profile.id) !== uuid) return null;
      if (String(profile.name).toLowerCase() !== name.toLowerCase()) return null;
      return profile;
    } catch {
      return null;
    }
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
        name: attachment.name
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

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/" || url.pathname === "/health") {
      return json({ service: "runal-presence", status: "ok" });
    }
    if (url.pathname !== "/presence") {
      return json({ error: "not found" }, 404);
    }

    const room = env.PRESENCE.getByName("global");
    return room.fetch(request);
  }
};
