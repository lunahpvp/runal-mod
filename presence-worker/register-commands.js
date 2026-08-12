// One-time (or re-run when commands change) registration of Runal's Discord slash
// commands. Needs DISCORD_BOT_TOKEN set in the environment - never hardcode it here.
//
// Usage (PowerShell):
//   $env:DISCORD_BOT_TOKEN = "paste-your-token-here"
//   node register-commands.js

const APPLICATION_ID = "1536969365625512056";
// Guild-scoped, not global - commands registered this way only exist inside this one
// server. They also propagate instantly, unlike global commands (up to an hour).
const GUILD_ID = "1523040301906792458";

const commands = [
  {
    name: "playerlist",
    description: "Show who's currently using Runal",
    type: 1
  },
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

  const response = await fetch(
    `https://discord.com/api/v10/applications/${APPLICATION_ID}/guilds/${GUILD_ID}/commands`,
    {
      method: "PUT",
      headers: {
        "Authorization": `Bot ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(commands)
    }
  );

  if (!response.ok) {
    console.error(`Registration failed: ${response.status} ${await response.text()}`);
    process.exit(1);
  }

  console.log("Registered commands:", (await response.json()).map(c => c.name).join(", "));
}

main();
