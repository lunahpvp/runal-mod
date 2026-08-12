// One-time (or re-run when commands change) registration of Runal's Discord slash
l
//
// Usage (PowerShell):
//   $env:DISCORD_BOT_TOKEN = "paste-your-token-here"
//   node register-commands.js

const APPLICATION_ID = "1536969365625512056";
// Guild-scoped, not global - commands registered this way only exist inside this one
// server. They also propagate instantly, unlike global commands (up to an hour).
const GUILD_ID = "1523040301906792458";

// Registered globally, and as a user-installable app (integration_types: 1) rather than a
// guild-installable one - this lets Lynaah personally install the app to their own Discord
// account (Developer Portal -> Installation -> enable "User Install") and run /playerlist in
// any server or DM from their own context, without the bot itself ever joining another
// server or being addable by anyone else. It's read-only, so there's no real downside to
// making it reachable that way. Global commands can take up to an hour to propagate, unlike
// guild-scoped ones below.
const globalCommands = [
  {
    name: "playerlist",
    description: "Show who's currently using Runal",
    type: 1,
    integration_types: [0, 1], // GUILD_INSTALL + USER_INSTALL
    contexts: [0, 1, 2] // guild channels, bot DMs, group/private channels
  }
];

// Guild-scoped, not global - these only exist inside this one server, and propagate
// instantly. ban/unban actually change state, so they stay locked down here.
const guildCommands = [
  {
    name: "ban",
    description: "Ban a player from Runal's online features (presence, Runal Chat)",
    type: 1,
    // No default_member_permissions here - the worker itself checks for the specific
    // "ban" role instead, since that's not the same thing as Discord's Administrator bit.
    options: [
      {
        name: "player",
        description: "Minecraft username or UUID",
        type: 3, // STRING
        required: true
      },
      {
        name: "reason",
        description: "Shown to the player when Runal refuses to open for them",
        type: 3, // STRING
        required: false
      }
    ]
  },
  {
    name: "unban",
    description: "Unban a player from Runal's online features",
    type: 1,
    // No default_member_permissions here - the worker itself checks for the specific
    // "ban" role instead, since that's not the same thing as Discord's Administrator bit.
    options: [
      {
        name: "player",
        description: "Minecraft username or UUID",
        type: 3, // STRING
        required: true
      }
    ]
  }
];

async function main() {
  const token = process.env.DISCORD_BOT_TOKEN;
  if (!token) {
    console.error("Set DISCORD_BOT_TOKEN in your environment first.");
    process.exit(1);
  }

  const headers = {
    "Authorization": `Bot ${token}`,
    "Content-Type": "application/json"
  };

  const guildResponse = await fetch(
    `https://discord.com/api/v10/applications/${APPLICATION_ID}/guilds/${GUILD_ID}/commands`,
    { method: "PUT", headers, body: JSON.stringify(guildCommands) }
  );
  if (!guildResponse.ok) {
    console.error(`Guild registration failed: ${guildResponse.status} ${await guildResponse.text()}`);
    process.exit(1);
  }
  console.log("Registered guild commands:", (await guildResponse.json()).map(c => c.name).join(", "));

  const globalResponse = await fetch(
    `https://discord.com/api/v10/applications/${APPLICATION_ID}/commands`,
    { method: "PUT", headers, body: JSON.stringify(globalCommands) }
  );
  if (!globalResponse.ok) {
    console.error(`Global registration failed: ${globalResponse.status} ${await globalResponse.text()}`);
    process.exit(1);
  }
  console.log("Registered global commands:", (await globalResponse.json()).map(c => c.name).join(", "));
}

main();
