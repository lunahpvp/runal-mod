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
// Not secret - Discord's public key, used to verify that interactions really came from
// Discord (every request is Ed25519-signed). The bot token, which IS secret, is never
// needed at runtime - only for the one-time slash command registration script.
const DISCORD_PUBLIC_KEY = "f1bdd7fd546b6f64ccdf6613c75847c434520ff2597def5747a05e76108810b6";
// The bot is locked to this one Discord server, and /ban+/unban to this one role, at the
// interaction-handler level - this holds even if "Public Bot" were ever left on by mistake
// in the Developer Portal, since Discord still routes every interaction through here first.
const ALLOWED_GUILD_ID = "1523040301906792458";
const BAN_ROLE_ID = "1523041064884375832";

// Boss -> region, for the /bosses command. Bosses not listed here are silently ignored by
// handleBossKill - the client just reports every "X Slain!" it sees and lets this list decide
// what's actually worth tracking, so adding a new boss later needs no mod update.
const BOSS_REGIONS = {
  "Alpha Wolf": "Plains",
  "Shade": "Plains",
  "Elemental Sheep": "Plains",
  "Fallen Minerian": "Plains",
  "The Shatterer": "Desert",
  "Angel of Death": "Desert",
  "Duselation": "Desert",
  "Bronze Guardian": "Desert",
  "Zerion": "Desert",
  "Servant": "Desert",
  "The Pharaoh": "Desert",
  "Quartz King": "Lower Sky",
  "Angel of Storms": "Lower Sky",
  "Delta": "Lower Sky",
  "Zerathis": "Upper Sky",
  "Pyros": "Upper Sky",
  "Zephyr, Commander of the Skies": "Upper Sky",
  "Acryl": "Upper Sky",
  "Skylord": "Upper Sky"
};
const REGIONS = ["Plains", "Desert", "Lower Sky", "Upper Sky"];

// Custom emoji per boss, shown in the /bosses region drill-down. <:name:id> is the actual
// markup Discord needs to render a custom emoji in a bot-sent message - plain :name: text
// only auto-completes in the client's own compose box, it's inert coming from the API.
const BOSS_EMOJIS = {
  "Acryl": "<:acryl:1538113816087502928>",
  "Alpha Wolf": "<:alphawolf:1538109180001390692>",
  "Shade": "<:shade:1538109329759019059>",
  "Elemental Sheep": "<:elementalsheep:1538109465772163193>",
  "Fallen Minerian": "<:fallenminerian:1538109612715282534>",
  "The Shatterer": "<:TheShatterer:1538109918836564009>",
  "Angel of Death": "<:angelofdeath:1538110110730289152>",
  "Duselation": "<:duselation:1538110215268995102>",
  "Bronze Guardian": "<:bronzeguardian:1538110378670817350>",
  "Zerion": "<:zerion:1538110761128427621>",
  "Servant": "<:servant:1538110983258505276>",
  "The Pharaoh": "<:thepharoah:1538111101999386664>",
  "Quartz King": "<:quartzking:1538111461753233448>",
  "Angel of Storms": "<:angelofstorms:1538111597241700414>",
  "Delta": "<:delta:1538111813022126101>",
  "Zerathis": "<:zerathis:1538112140207071292>",
  "Pyros": "<:pyros:1538112825598148628>",
  "Zephyr, Commander of the Skies": "<:ZephyrCommanderoftheSkies:1538113104620167168>",
  "Skylord": "<:skylord:1538113387870031915>"
};

const REGION_EMOJIS = {
  "Plains": "<:plains:1538135110627631175>",
  "Desert": "<:desert:1538135618389938226>",
  "Lower Sky": "<:lowersky:1538136004383350854>",
  "Upper Sky": "<:uppersky:1538136616890015744>"
};

const RARITY_ORDER = ["rare", "epic", "legendary", "mythical"];
const RARITY_EMOJIS = {
  rare: "<:rare:1538134110374203392>",
  epic: "<:epic:1538133463599812620>",
  legendary: "<:legendary:1538133884129251358>",
  mythical: "<:mythical:1538134667604271126>"
};
const TROPHY_EMOJI = "<:trophy:1538119254170275860>";

function capitalize(word) {
  return word.charAt(0).toUpperCase() + word.slice(1);
}

function formatPlaytime(totalSeconds) {
  const totalHours = totalSeconds / 3600;
  const weeks = Math.floor(totalSeconds / (7 * 24 * 3600));
  let remaining = totalSeconds - weeks * 7 * 24 * 3600;
  const days = Math.floor(remaining / (24 * 3600));
  remaining -= days * 24 * 3600;
  const hours = Math.floor(remaining / 3600);

  const parts = [];
  if (weeks > 0) parts.push(`${weeks} week${weeks === 1 ? "" : "s"}`);
  if (days > 0) parts.push(`${days} day${days === 1 ? "" : "s"}`);
  if (hours > 0 || parts.length === 0) parts.push(`${hours} hour${hours === 1 ? "" : "s"}`);

  return `${parts.join(", ")} (${totalHours.toFixed(2)} hours)`;
}

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

const CHAT_COLORS = new Set(["1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "e", "f"]);
function isValidChatColor(value) {
  return typeof value === "string" && CHAT_COLORS.has(value);
}

export class PresenceRoom {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.discordSocket = null;
    this.discordSeq = null;
    this.discordSessionId = null;
    this.discordResumeUrl = null;
    this.discordHeartbeatIntervalMs = null;

    // Durable Object SQLite storage - unlike ctx.storage.get/put (KV-style), this survives
    // mod updates and worker redeploys since it's the actual persistent backing store, not
    // anything tied to a specific deployed version of this file.
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS boss_kills (
        uuid TEXT NOT NULL,
        boss TEXT NOT NULL,
        count INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (uuid, boss)
      )
    `);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS item_drops (
        uuid TEXT NOT NULL,
        rarity TEXT NOT NULL,
        count INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (uuid, rarity)
      )
    `);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS boss_drops (
        uuid TEXT NOT NULL,
        boss TEXT NOT NULL,
        rarity TEXT NOT NULL,
        count INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (uuid, boss, rarity)
      )
    `);
    // "deaths" here really means raid deaths - the "X has died!" broadcast this is detected
    // from only fires during raid fights, not from every death on the server.
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS player_stats (
        uuid TEXT PRIMARY KEY,
        total_playtime_seconds INTEGER NOT NULL DEFAULT 0,
        last_seen_at INTEGER,
        raid_deaths INTEGER NOT NULL DEFAULT 0,
        scrolls INTEGER NOT NULL DEFAULT 0,
        level INTEGER
      )
    `);
    // CREATE TABLE IF NOT EXISTS above is a no-op once the table already exists from an
    // earlier deploy, so it doesn't retroactively add columns added since - this backfills
    // them onto whatever's already live. Safe to run every time: the second+ run just throws
    // "duplicate column" once the column's already there, which is ignored.
    try {
      this.ctx.storage.sql.exec(`ALTER TABLE player_stats ADD COLUMN level INTEGER`);
    } catch {
      // Already has the column.
    }
  }

  // Kicks the Discord gateway connection off (or back up) whenever this object wakes for
  // any real event - not just fetch(), which barely ever runs once a player's WebSocket is
  // already open (every ping/chat/refresh after that goes through webSocketMessage
  // instead). Call this from every entry point that can run after a hibernation cycle.
  // (Not done from the constructor: waitUntil isn't valid there, only inside an actual
  // event's execution context, so a constructor-time call silently never ran.)
  kickDiscordGateway() {
    if (this.env.DISCORD_BOT_TOKEN) {
      this.ctx.waitUntil(this.ensureDiscordGateway());
    }
  }

  async fetch(request) {
    this.kickDiscordGateway();

    // These two paths are only ever reached via a direct Durable Object stub call from
    // this same worker's own code (the Discord interaction handler below) - they're not
    // routed to from the public default export, so they're not reachable from outside.
    const url = new URL(request.url);
    if (url.pathname === "/internal/players") {
      return this.handleInternalPlayerList();
    }
    if (url.pathname === "/internal/ban") {
      return this.handleInternalBan(request);
    }
    if (url.pathname === "/internal/ban-status") {
      return this.handleInternalBanStatus(url.searchParams.get("uuid"));
    }
    if (url.pathname === "/internal/boss-kills") {
      return this.handleInternalBossKills(url.searchParams.get("uuid"));
    }
    if (url.pathname === "/internal/boss-drops") {
      return this.handleInternalBossDrops(url.searchParams.get("uuid"));
    }
    if (url.pathname === "/internal/player-stats") {
      return this.handleInternalPlayerStats(url.searchParams.get("uuid"));
    }
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
    this.kickDiscordGateway();

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

    let attachment = socket.deserializeAttachment() || {};
    const isRefreshResponse = attachment.authenticated
      && message.action === "auth_response"
      && attachment.pendingServerId;

    if (attachment.authenticated && !isRefreshResponse) {
      // Flush accumulated playtime + refresh last_seen_at on every authenticated message, not
      // just refresh - this way a Durable Object restart loses at most a few seconds of
      // unflushed time instead of a whole session's worth.
      const flushedAt = this.touchPlaytime(attachment.uuid, attachment);
      attachment = { ...attachment, playtimeFlushedAt: flushedAt };
      socket.serializeAttachment(attachment);

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
      } else if (message.action === "boss_kill") {
        this.handleBossKill(attachment, message);
      } else if (message.action === "item_drop") {
        this.handleItemDrop(attachment, message);
      } else if (message.action === "raid_death") {
        this.handleRaidDeath(attachment);
      } else if (message.action === "scroll_found") {
        this.handleScrollFound(attachment);
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

    const bans = await this.getBans();
    if (bans[uuid]) {
      console.warn(`Closing socket: banned uuid=${uuid}`);
      socket.close(4403, "banned");
      return;
    }

    console.log(`${isRefreshResponse ? "Refreshed" : "Accepted"} presence claim for ${name} (${uuid})`);

    const authedAt = Date.now();
    socket.serializeAttachment({
      authenticated: true,
      uuid,
      name,
      serverId,
      server: String(message.server || "unknown").slice(0, 64),
      color: isValidChatColor(message.color) ? message.color : "f",
      version: String(message.version || "unknown").slice(0, 32),
      connectedAt: attachment.connectedAt,
      lastRefreshAt: Date.now(),
      playtimeFlushedAt: authedAt
    });
    this.touchPlaytime(uuid, { playtimeFlushedAt: authedAt });
    this.updateLevel(uuid, message.level);
    socket.send(JSON.stringify({ action: "auth_success" }));
    this.broadcastSnapshot();
  }

  // Sent on every auth (connect + ~15s refresh cycle) rather than as its own message type -
  // vanilla's XP level number, which this server repurposes as the RPG level, changes rarely
  // enough that piggybacking on the existing periodic auth traffic is plenty fresh.
  updateLevel(uuid, level) {
    const parsed = Number(level);
    if (!Number.isInteger(parsed) || parsed < 0 || parsed > 100_000) return;

    this.ctx.storage.sql.exec(
      `INSERT INTO player_stats (uuid, level) VALUES (?, ?)
       ON CONFLICT (uuid) DO UPDATE SET level = excluded.level`,
      uuid,
      parsed
    );
  }

  async getMutes() {
    return (await this.ctx.storage.get("mutedUsers")) || {};
  }

  async setMutes(mutes) {
    await this.ctx.storage.put("mutedUsers", mutes);
  }

  async getBans() {
    return (await this.ctx.storage.get("bannedUsers")) || {};
  }

  async setBans(bans) {
    await this.ctx.storage.put("bannedUsers", bans);
  }

  async handleInternalPlayerList() {
    const seen = new Set();
    const players = [];
    for (const socket of this.ctx.getWebSockets()) {
      const attachment = socket.deserializeAttachment() || {};
      if (!attachment.authenticated || !attachment.uuid || seen.has(attachment.uuid)) continue;
      seen.add(attachment.uuid);
      players.push({ name: attachment.name, uuid: attachment.uuid, server: attachment.server || "unknown" });
    }
    return json({ players });
  }

  async handleInternalBan(request) {
    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "invalid json" }, 400);
    }

    const uuid = normalizeUuid(body.uuid);
    if (!isValidUuid(uuid)) return json({ error: "invalid uuid" }, 400);

    const bans = await this.getBans();
    if (body.banned) {
      const reason = String(body.reason || "No reason given").slice(0, 256);
      bans[uuid] = { at: Date.now(), name: body.name || null, reason };
    } else {
      delete bans[uuid];
    }
    await this.setBans(bans);

    if (body.banned) {
      for (const socket of this.ctx.getWebSockets()) {
        const attachment = socket.deserializeAttachment() || {};
        if (attachment.uuid === uuid) {
          try {
            socket.close(4403, "banned");
          } catch {
            // Already closing/closed.
          }
        }
      }
    }

    return json({ ok: true });
  }

  async handleInternalBanStatus(uuid) {
    const bans = await this.getBans();
    const ban = bans[normalizeUuid(uuid)];
    if (!ban) return json({ banned: false });
    return json({ banned: true, reason: ban.reason || "No reason given" });
  }

  // attachment.uuid is already server-verified (Mojang session-join, same as presence auth
  // above) so this is trusted without any extra check here.
  handleBossKill(attachment, message) {
    const boss = String(message.boss || "").trim().slice(0, 64);
    if (!boss || !BOSS_REGIONS[boss]) return;

    this.ctx.storage.sql.exec(
      `INSERT INTO boss_kills (uuid, boss, count) VALUES (?, ?, 1)
       ON CONFLICT (uuid, boss) DO UPDATE SET count = count + 1`,
      attachment.uuid,
      boss
    );
  }

  handleInternalBossKills(uuid) {
    const normalized = normalizeUuid(uuid);
    if (!isValidUuid(normalized)) return json({ error: "invalid uuid" }, 400);

    const rows = this.ctx.storage.sql
      .exec(`SELECT boss, count FROM boss_kills WHERE uuid = ?`, normalized)
      .toArray();
    const kills = {};
    for (const row of rows) kills[row.boss] = row.count;
    return json({ uuid: normalized, kills });
  }

  handleItemDrop(attachment, message) {
    const rarity = String(message.rarity || "").trim().toLowerCase();
    if (!RARITY_ORDER.includes(rarity)) return;

    this.ctx.storage.sql.exec(
      `INSERT INTO item_drops (uuid, rarity, count) VALUES (?, ?, 1)
       ON CONFLICT (uuid, rarity) DO UPDATE SET count = count + 1`,
      attachment.uuid,
      rarity
    );

    const boss = String(message.boss || "").trim();
    if (boss && BOSS_REGIONS[boss]) {
      this.ctx.storage.sql.exec(
        `INSERT INTO boss_drops (uuid, boss, rarity, count) VALUES (?, ?, ?, 1)
         ON CONFLICT (uuid, boss, rarity) DO UPDATE SET count = count + 1`,
        attachment.uuid,
        boss,
        rarity
      );
    }
  }

  handleRaidDeath(attachment) {
    this.ctx.storage.sql.exec(
      `INSERT INTO player_stats (uuid, raid_deaths) VALUES (?, 1)
       ON CONFLICT (uuid) DO UPDATE SET raid_deaths = raid_deaths + 1`,
      attachment.uuid
    );
  }

  handleScrollFound(attachment) {
    this.ctx.storage.sql.exec(
      `INSERT INTO player_stats (uuid, scrolls) VALUES (?, 1)
       ON CONFLICT (uuid) DO UPDATE SET scrolls = scrolls + 1`,
      attachment.uuid
    );
  }

  // Flushes elapsed connected time into total_playtime_seconds and bumps last_seen_at.
  // Called on every authenticated message (not just refresh) so a Durable Object restart
  // only loses a few seconds of unflushed time, and again on close/error for the final chunk.
  // Returns the new flush baseline to store back on the connection's attachment.
  touchPlaytime(uuid, attachment) {
    const now = Date.now();
    const flushedAt = Number(attachment.playtimeFlushedAt || attachment.connectedAt || now);
    const elapsedSeconds = Math.max(0, Math.floor((now - flushedAt) / 1000));

    this.ctx.storage.sql.exec(
      `INSERT INTO player_stats (uuid, total_playtime_seconds, last_seen_at) VALUES (?, ?, ?)
       ON CONFLICT (uuid) DO UPDATE SET
         total_playtime_seconds = total_playtime_seconds + excluded.total_playtime_seconds,
         last_seen_at = excluded.last_seen_at`,
      uuid,
      elapsedSeconds,
      now
    );
    return now;
  }

  handleInternalBossDrops(uuid) {
    const normalized = normalizeUuid(uuid);
    if (!isValidUuid(normalized)) return json({ error: "invalid uuid" }, 400);

    const rows = this.ctx.storage.sql
      .exec(`SELECT boss, rarity, count FROM boss_drops WHERE uuid = ?`, normalized)
      .toArray();
    const bossDrops = {};
    for (const row of rows) {
      if (!bossDrops[row.boss]) bossDrops[row.boss] = {};
      bossDrops[row.boss][row.rarity] = row.count;
    }
    return json({ uuid: normalized, bossDrops });
  }

  handleInternalPlayerStats(uuid) {
    const normalized = normalizeUuid(uuid);
    if (!isValidUuid(normalized)) return json({ error: "invalid uuid" }, 400);

    const statsRows = this.ctx.storage.sql
      .exec(
        `SELECT total_playtime_seconds, last_seen_at, raid_deaths, scrolls, level FROM player_stats WHERE uuid = ?`,
        normalized
      )
      .toArray();
    const stats = statsRows[0] || {
      total_playtime_seconds: 0,
      last_seen_at: null,
      raid_deaths: 0,
      scrolls: 0,
      level: null
    };

    const dropRows = this.ctx.storage.sql
      .exec(`SELECT rarity, count FROM item_drops WHERE uuid = ?`, normalized)
      .toArray();
    const drops = {};
    for (const row of dropRows) drops[row.rarity] = row.count;

    return json({ uuid: normalized, stats, drops });
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

    const payload = JSON.stringify({ action: "chat", name: attachment.name, server, text, color: attachment.color || "f" });
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

  // --- Discord gateway (bot online status + "Helping X Players") ---
  //
  // Slash commands work over plain HTTP webhooks, but a bot's online/idle status and
  // custom activity text are gateway-only features - there's no REST equivalent. This
  // hand-rolls the minimal gateway client needed just for that: identify, heartbeat,
  // presence update. No message/event intents are requested since nothing here needs to
  // read anything from Discord, only announce a status.
  //
  // Durable Objects can get evicted from memory when idle, which would silently kill a
  // plain in-memory WebSocket like this one - the alarm below exists purely to wake this
  // object back up often enough to notice and reconnect if that happens.

  currentPlayerCount() {
    const seen = new Set();
    for (const socket of this.ctx.getWebSockets()) {
      const attachment = socket.deserializeAttachment() || {};
      if (attachment.authenticated && attachment.uuid) seen.add(attachment.uuid);
    }
    return seen.size;
  }

  async ensureDiscordGateway() {
    if (this.discordSocket && this.discordSocket.readyState === 1) return;
    console.log("Discord gateway: connecting, token present =", !!this.env.DISCORD_BOT_TOKEN);
    // Belt-and-suspenders: if this object gets evicted mid-connect (before open/close/error
    // ever fires on the plain outbound socket below, which - unlike the player-facing
    // sockets - isn't hibernation-aware), nothing would otherwise ever wake this object up
    // again to retry. A short fallback alarm guarantees a retry regardless of what happens
    // to the in-flight connection; a successful HELLO handshake reschedules it to the real
    // heartbeat interval once one exists.
    await this.ctx.storage.setAlarm(Date.now() + 10_000);
    try {
      const url = this.discordResumeUrl || "wss://gateway.discord.gg/?v=10&encoding=json";
      const socket = new WebSocket(url);
      this.discordSocket = socket;
      socket.addEventListener("open", () => console.log("Discord gateway: socket opened"));
      socket.addEventListener("message", event => this.handleDiscordGatewayMessage(event));
      socket.addEventListener("close", event => {
        console.warn("Discord gateway: closed", event.code, event.reason);
        this.discordSocket = null;
        this.ctx.storage.setAlarm(Date.now() + 5_000);
      });
      socket.addEventListener("error", event => {
        console.warn("Discord gateway: socket error", event.message || event);
      });
    } catch (err) {
      console.warn("Discord gateway connect failed:", err.message, err.stack);
      await this.ctx.storage.setAlarm(Date.now() + 15_000);
    }
  }

  handleDiscordGatewayMessage(event) {
    let payload;
    try {
      payload = JSON.parse(event.data);
    } catch {
      return;
    }
    if (payload.s != null) this.discordSeq = payload.s;

    if (payload.op === 10) {
      console.log("Discord gateway: HELLO received, heartbeat interval =", payload.d.heartbeat_interval);
      this.discordHeartbeatIntervalMs = payload.d.heartbeat_interval;
      this.ctx.storage.setAlarm(Date.now() + this.discordHeartbeatIntervalMs);
      if (this.discordSessionId) {
        this.sendDiscordPayload({
          op: 6,
          d: {
            token: this.env.DISCORD_BOT_TOKEN,
            session_id: this.discordSessionId,
            seq: this.discordSeq
          }
        });
      } else {
        this.sendDiscordPayload({
          op: 2,
          d: {
            token: this.env.DISCORD_BOT_TOKEN,
            intents: 0,
            properties: { os: "linux", browser: "runal-presence", device: "runal-presence" }
          }
        });
      }
      return;
    }

    if (payload.op === 0 && payload.t === "READY") {
      console.log("Discord gateway: READY, session started");
      this.discordSessionId = payload.d.session_id;
      this.discordResumeUrl = payload.d.resume_gateway_url;
      this.updateDiscordPresence();
      return;
    }

    if (payload.op === 9) {
      console.warn("Discord gateway: INVALID_SESSION, resumable =", payload.d);
    }

    if (payload.op === 1) {
      this.sendDiscordPayload({ op: 1, d: this.discordSeq });
      return;
    }

    if (payload.op === 7) {
      this.discordSocket?.close();
      return;
    }

    if (payload.op === 9) {
      this.discordSessionId = null;
      this.discordResumeUrl = null;
      this.discordSocket?.close();
    }
  }

  sendDiscordPayload(payload) {
    if (this.discordSocket && this.discordSocket.readyState === 1) {
      try {
        this.discordSocket.send(JSON.stringify(payload));
      } catch {
        // The close/error listener handles reconnecting.
      }
    }
  }

  updateDiscordPresence() {
    const count = this.currentPlayerCount();
    const text = `Helping ${count} ${count === 1 ? "Player" : "Players"}`;
    this.sendDiscordPayload({
      op: 3,
      d: {
        since: null,
        status: "online",
        afk: false,
        activities: [{ name: text, type: 4, state: text }]
      }
    });
  }

  async alarm() {
    if (!this.discordSocket || this.discordSocket.readyState !== 1) {
      await this.ensureDiscordGateway();
      return;
    }
    this.sendDiscordPayload({ op: 1, d: this.discordSeq });
    this.ctx.storage.setAlarm(Date.now() + (this.discordHeartbeatIntervalMs || 30_000));
  }

  webSocketClose(ws) {
    this.flushPlaytimeOnDisconnect(ws);
    this.broadcastSnapshot();
  }

  webSocketError(ws) {
    this.flushPlaytimeOnDisconnect(ws);
    this.broadcastSnapshot();
  }

  flushPlaytimeOnDisconnect(ws) {
    const attachment = ws.deserializeAttachment() || {};
    if (attachment.authenticated && attachment.uuid) {
      this.touchPlaytime(attachment.uuid, attachment);
    }
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

    this.updateDiscordPresence();
  }
}

function hexToBytes(hex) {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.substr(i * 2, 2), 16);
  }
  return bytes;
}

async function verifyDiscordSignature(request, rawBody) {
  const signature = request.headers.get("X-Signature-Ed25519");
  const timestamp = request.headers.get("X-Signature-Timestamp");
  if (!signature || !timestamp) return false;

  try {
    const key = await crypto.subtle.importKey(
      "raw",
      hexToBytes(DISCORD_PUBLIC_KEY),
      { name: "Ed25519" },
      false,
      ["verify"]
    );
    return await crypto.subtle.verify(
      "Ed25519",
      key,
      hexToBytes(signature),
      new TextEncoder().encode(timestamp + rawBody)
    );
  } catch {
    return false;
  }
}

// Confirmed via wrangler tail: both direct Mojang endpoints return 403 + an HTML WAF
// challenge page (not Mojang's own JSON error) for every request from this worker - Mojang's
// front door blocks Cloudflare Workers' shared egress IPs outright, not just rate-limits them,
// so a real User-Agent header alone doesn't help. mowojang mirrors the same lookup from
// infrastructure that isn't blocked and returns the identical { id, name } shape, so it's
// tried first for latency; the direct endpoints stay as fallback in case the block ever lifts.
const MOJANG_LOOKUP_HEADERS = {
  "User-Agent": "runal-presence-worker",
  "Accept": "application/json"
};
const MOJANG_LOOKUP_ENDPOINTS = username => [
  `https://mowojang.matdoes.dev/${encodeURIComponent(username)}`,
  `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(username)}`,
  `https://api.minecraftservices.com/minecraft/profile/lookup/name/${encodeURIComponent(username)}`
];

async function lookupUuidByUsername(username) {
  let sawNotFound = false;
  for (const endpoint of MOJANG_LOOKUP_ENDPOINTS(username)) {
    try {
      const response = await fetch(endpoint, { headers: MOJANG_LOOKUP_HEADERS });
      if (response.status === 404) {
        sawNotFound = true;
        continue;
      }
      if (!response.ok) {
        console.warn(`Mojang lookup ${endpoint} -> ${response.status}: ${(await response.text()).slice(0, 200)}`);
        continue;
      }
      const data = await response.json();
      if (data?.id) return { id: data.id, name: data.name };
      console.warn(`Mojang lookup ${endpoint} -> ok but no id in body: ${JSON.stringify(data).slice(0, 200)}`);
    } catch (err) {
      console.warn(`Mojang lookup ${endpoint} threw: ${err.message}`);
    }
  }
  return { error: sawNotFound ? "not_found" : "lookup_failed" };
}

// Returns either { uuid, name } or { error: "not_found" | "lookup_failed" } - callers use the
// error to tell the user "no such player" apart from "Mojang's lookup is down right now",
// instead of both looking identical like they used to.
async function resolveTargetPlayer(target) {
  const raw = String(target || "").trim();
  if (isValidUuid(raw)) return { uuid: normalizeUuid(raw), name: null };
  if (!isValidName(raw)) return { error: "not_found" };

  const lookup = await lookupUuidByUsername(raw);
  if (lookup.error) return lookup;
  return { uuid: normalizeUuid(lookup.id), name: lookup.name };
}

function playerLookupFailureMessage(error) {
  return error === "lookup_failed"
    ? "Mojang's player lookup is having issues right now - try again shortly, or use their UUID instead."
    : "Couldn't find that player - check the username or UUID.";
}

async function handleDiscordInteraction(request, env) {
  const rawBody = await request.text();
  if (!(await verifyDiscordSignature(request, rawBody))) {
    return new Response("invalid request signature", { status: 401 });
  }

  const interaction = JSON.parse(rawBody);
  if (interaction.type === 1) {
    return json({ type: 1 });
  }
  if (interaction.type === 3) {
    return handleMessageComponent(interaction, env);
  }
  if (interaction.type !== 2) {
    return json({ error: "unsupported interaction type" }, 400);
  }

  const room = env.PRESENCE.getByName("global");
  const commandName = interaction.data?.name;

  // playerlist is registered globally and works in any server the bot's been added to -
  // it's read-only, so there's nothing here worth locking down. ban/unban stay restricted
  // to the home server below, since those actually change state.
  if (commandName === "playerlist") {
    const response = await room.fetch("https://internal/internal/players");
    const { players } = await response.json();
    const content = players.length === 0
      ? "Nobody is currently using Runal."
      : `**${players.length} ${players.length === 1 ? "Player" : "Players"} using Runal right now:**\n`
        + players.map(p => `- ${p.name} (${p.server})`).join("\n");
    return json({ type: 4, data: { content } });
  }

  if (commandName === "ban" || commandName === "unban") {
    if (interaction.guild_id !== ALLOWED_GUILD_ID) {
      return json({
        type: 4,
        data: { content: "This bot doesn't work outside its home server.", flags: 64 }
      });
    }

    const memberRoles = interaction.member?.roles || [];
    if (!memberRoles.includes(BAN_ROLE_ID)) {
      return json({
        type: 4,
        data: { content: "You don't have permission to do that.", flags: 64 }
      });
    }

    const targetOption = interaction.data.options?.find(option => option.name === "player");
    const resolved = await resolveTargetPlayer(targetOption?.value);
    if (resolved.error) {
      return json({
        type: 4,
        data: { content: playerLookupFailureMessage(resolved.error), flags: 64 }
      });
    }

    const banning = commandName === "ban";
    const reasonOption = interaction.data.options?.find(option => option.name === "reason");
    await room.fetch("https://internal/internal/ban", {
      method: "POST",
      body: JSON.stringify({
        uuid: resolved.uuid,
        name: resolved.name,
        banned: banning,
        reason: reasonOption?.value
      })
    });

    const label = resolved.name || resolved.uuid;
    const content = banning
      ? `Banned **${label}** from Runal${reasonOption?.value ? ` for: ${reasonOption.value}` : ""}.`
      : `Unbanned **${label}**.`;
    return json({ type: 4, data: { content } });
  }

  if (commandName === "bosses") {
    const targetOption = interaction.data.options?.find(option => option.name === "player");
    const resolved = await resolveTargetPlayer(targetOption?.value);
    if (resolved.error) {
      return json({
        type: 4,
        data: { content: playerLookupFailureMessage(resolved.error), flags: 64 }
      });
    }

    const [killsResponse, statsResponse] = await Promise.all([
      room.fetch(`https://internal/internal/boss-kills?uuid=${resolved.uuid}`),
      room.fetch(`https://internal/internal/player-stats?uuid=${resolved.uuid}`)
    ]);
    const { kills } = await killsResponse.json();
    const { drops } = await statsResponse.json();
    const label = resolved.name || resolved.uuid;
    return json({ type: 4, data: buildBossKillsMessage(label, resolved.uuid, kills, drops) });
  }

  if (commandName === "general") {
    const targetOption = interaction.data.options?.find(option => option.name === "player");
    const resolved = await resolveTargetPlayer(targetOption?.value);
    if (resolved.error) {
      return json({
        type: 4,
        data: { content: playerLookupFailureMessage(resolved.error), flags: 64 }
      });
    }

    const response = await room.fetch(`https://internal/internal/player-stats?uuid=${resolved.uuid}`);
    const { stats } = await response.json();
    const label = resolved.name || resolved.uuid;
    return json({ type: 4, data: buildGeneralMessage(label, stats) });
  }

  return json({ error: "unknown command" }, 400);
}

function buildGeneralMessage(label, stats) {
  const lastSeenLine = stats.last_seen_at
    ? `<t:${Math.floor(stats.last_seen_at / 1000)}:D>`
    : "Never";
  const lines = [
    `**Last Seen:** ${lastSeenLine}`,
    `**Playtime:** ${formatPlaytime(stats.total_playtime_seconds)}`,
    `**Level:** ${stats.level ?? "Unknown"}`,
    `**Scrolls:** ${stats.scrolls}`,
    `**Raid Deaths:** ${stats.raid_deaths}`
  ];
  return {
    embeds: [{
      title: `${label}'s Profile`,
      description: lines.join("\n"),
      color: 0x57f287
    }]
  };
}

function regionTotal(kills, region) {
  let total = 0;
  for (const [boss, count] of Object.entries(kills)) {
    if (BOSS_REGIONS[boss] === region) total += count;
  }
  return total;
}

function regionButtons(uuid) {
  return [{
    type: 1,
    components: REGIONS.map(region => ({
      type: 2,
      style: 1,
      label: region,
      custom_id: `bosses_region:${uuid}:${region}`
    }))
  }];
}

function buildBossKillsMessage(label, uuid, kills, drops) {
  const total = Object.values(kills).reduce((sum, count) => sum + count, 0);
  const dropLines = RARITY_ORDER.map(rarity =>
    `${RARITY_EMOJIS[rarity]}: ${drops[rarity] || 0} ${capitalize(rarity)} Drops`
  );
  const regionLines = REGIONS.map(region => {
    const emoji = REGION_EMOJIS[region];
    const prefix = emoji ? `${emoji}: ` : "";
    return `${prefix}${regionTotal(kills, region)} ${region} Kills`;
  });
  return {
    embeds: [{
      title: `${label}'s Boss Kills`,
      description: `${TROPHY_EMOJI} **Total Kills:** ${total}\n${dropLines.join("\n")}\n\n${regionLines.join("\n")}`,
      color: 0x57f287
    }],
    components: regionButtons(uuid)
  };
}

function buildRegionMessage(label, uuid, region, kills, bossDrops) {
  const bosses = Object.keys(BOSS_REGIONS).filter(boss => BOSS_REGIONS[boss] === region);
  const lines = bosses.map(boss => {
    const emoji = BOSS_EMOJIS[boss];
    const prefix = emoji ? `${emoji}: ` : "";
    const drops = bossDrops[boss] || {};
    const dropParts = RARITY_ORDER
      .filter(rarity => drops[rarity])
      .map(rarity => `${drops[rarity]} ${RARITY_EMOJIS[rarity]}`);
    const dropSuffix = dropParts.length > 0 ? ` (${dropParts.join("/")})` : "";
    return `${prefix}${kills[boss] || 0} ${boss} Kills${dropSuffix}`;
  });
  return {
    embeds: [{
      title: `${label}'s ${region} Boss Kills`,
      description: lines.join("\n") || "No bosses tracked for this region yet.",
      color: 0x57f287
    }],
    components: regionButtons(uuid)
  };
}

async function handleMessageComponent(interaction, env) {
  const customId = interaction.data?.custom_id || "";
  const [kind, uuid, region] = customId.split(":");
  if (kind !== "bosses_region" || !isValidUuid(uuid) || !REGIONS.includes(region)) {
    return json({ error: "unknown component" }, 400);
  }

  const room = env.PRESENCE.getByName("global");
  const [killsResponse, dropsResponse] = await Promise.all([
    room.fetch(`https://internal/internal/boss-kills?uuid=${uuid}`),
    room.fetch(`https://internal/internal/boss-drops?uuid=${uuid}`)
  ]);
  const { kills } = await killsResponse.json();
  const { bossDrops } = await dropsResponse.json();

  // The original embed's title already carries the player's name/uuid label; reuse it
  // instead of re-resolving it (no player option available on a component interaction).
  const previousTitle = interaction.message?.embeds?.[0]?.title || "";
  const label = previousTitle.replace(/'s .*$/, "") || uuid;

  return json({ type: 7, data: buildRegionMessage(label, uuid, region, kills, bossDrops) });
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
    if (url.pathname === "/discord-interactions") {
      return handleDiscordInteraction(request, env);
    }
    if (url.pathname === "/ban-status") {
      const uuid = url.searchParams.get("uuid");
      if (!isValidUuid(uuid || "")) return json({ error: "invalid uuid" }, 400);
      const room = env.PRESENCE.getByName("global");
      return room.fetch(`https://internal/internal/ban-status?uuid=${normalizeUuid(uuid)}`);
    }
    if (url.pathname !== "/presence") {
      return json({ error: "not found" }, 404);
    }

    const room = env.PRESENCE.getByName("global");
    return room.fetch(request);
  }
};
