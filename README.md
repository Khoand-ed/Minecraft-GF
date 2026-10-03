# MCGF AI Companion

> English | [Tiếng Việt](README.vi.md) | [日本語](README.ja.md) | [Français](README.fr.md)

A companion mod for **Minecraft 1.21.1 Java Edition (Fabric)**. Spawn a human-like buddy that follows you, fights with you, mines, farms, chats with AI, and carries its own inventory.

> Repo: https://github.com/Khoand-ed/Minecraft-GF
> MC `1.21.1` | Fabric Loader `0.16.14` | Fabric API `0.102.0+1.21.1` | Java `21`

## Requirements

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (only needed to build from source)

## Install

1. Download `mcgf-ai-companion-<version>.jar` from
   https://github.com/Khoand-ed/Minecraft-GF/releases (latest release).
2. Put it in `.minecraft/mods/` (Fabric 1.21.1 profile with Fabric API).
3. Launch the game, open a world with cheats enabled (or OP on a server).

## Quick start (in game)

```
/gf spawn    # summon your companion
/gf follow   # it follows and protects you
@gf hello    # chat with it (Vietnamese/English/Japanese, works without any key)
/gf lang en  # switch AI language: vi | en | ja
```

## Commands

### Companion

| Command | What it does |
|---|---|
| `/gf spawn` | Summon your companion (tamed to you, named, persistent across relogs) |
| `/gf follow` | Follow you (default behavior) |
| `/gf stay` | Stand still (sneak pose) |
| `/gf here` | Teleport it next to you |
| `/gf goto <x> <y> <z>` | Send it to coordinates, then stand still |
| `/gf dismiss` | Send it away |
| `/gf name <name>` | Rename it (also updates the nameplate) |
| `/gf help` | Full command list, grouped by feature |

If it gets lost far away (over `teleportDistance`, default 24 blocks) it teleports back by itself.

If it **dies**, its bag + equipped gear drop at the death spot (real survival rules) and you get the coordinates in chat.

| Command | What it does |
|---|---|
| `/gf revive` | Respawn it next to you (60s cooldown, drops stay where it died) |
| `/gf grave` | Show where it last died |

Without commands it also **auto-revives after 5 minutes** (`autoReviveMinutes` in config, `0` = off).

### Combat & survival

| Command | What it does |
|---|---|
| `/gf attack` | Attack the nearest hostile mob (16 blocks) |
| `/gf stop` | Stop fighting / cancel the current auto-job |
| `/gf mine` | Look at a block (within 6 blocks) and it mines it for you — real survival drops |
| `/gf collect` | Pull nearby dropped items + XP (10 blocks) into your inventory |
| `/gf feed` | Feed it meat from your inventory to heal it |

It also slowly regenerates health outside of combat. Meat heals more when cooked.

### Auto-work (it works block by block, ~1 block / 4 ticks)

| Command | What it does |
|---|---|
| `/gf minevein` | Mines a whole nearby ore vein (max 32 blocks, 24-block radius) |
| `/gf chop` | Chops a whole nearby tree |
| `/gf farm` | Harvests ripe crops nearby and replants them |
| `/gf bag` | Show its private 9-slot inventory |
| `/gf give` | Take everything from its inventory (overflow drops at your feet) |
| `/gf deposit` | Store its inventory into the nearest chest/barrel (8 blocks) |

Drops fall on the ground like real survival, then it picks them into its own inventory. Old wolf-form pets from earlier worlds convert automatically and keep their inventory.

### Gear (real stats, saved with the pet)

| Command | What it does |
|---|---|
| `/gf equip` | Equip the best weapon + armor from its bag first, then your inventory |
| `/gf gear` | Show equipped items, attack damage and armor points |
| `/gf unequip` | Take everything off, back to your inventory |

Attack damage and armor protection are real (vanilla damage system), and gear is rendered on its body: weapon in the right hand, armor per piece.

### AI chat

Talk with the prefix (default `@gf`) anywhere in chat:

```
@gf what is a creeper?
@gf where am I?
```

| Command | What it does |
|---|---|
| `/gf ask <question>` | Same as chatting with the prefix |
| `/gf forget` | Clear conversation memory |
| `/gf ai on\|off` | Toggle online AI (needs OP) |
| `/gf apikey <key>` | Save a free Gemini key (needs OP, key is masked) |

How it works: with a key it calls Gemini (async, no server lag) and knows your position, health and hunger plus recent conversation. Without a key (or if the network fails) it answers offline. AI language follows `/gf lang` (`vi` | `en` | `ja` | `fr`) for both online and offline replies. Memory is saved to `config/mcgf_history.json` on server stop and reloaded on start — each player has their own memory. Get a free key at https://aistudio.google.com/apikey.

### Misc

| Command | What it does |
|---|---|
| `/gf say <text>` | Make it repeat text |
| `/gf hello` | Greeting |
| `/gf config` | Show current settings |
| `/gf prefix <p>` | Change the chat prefix (needs OP) |
| `/gf lang vi\|en\|ja\|fr` | AI reply language (Vietnamese / English / Japanese / French) |
| `/gf version` | Show mod version |

## Skins

Your companion looks like a player (classic arms). Skin priority, no rebuild needed:

1. `config/mcgf_skin.png` — drop any 64x64 PNG skin in the config folder, relog.
2. `skinUrl` in `config/mcgf.json` — direct PNG link, used when no file exists.
3. Built-in default skin (blue hoodie + jeans).

## Config (`config/mcgf.json`)

```json
{
  "companionName": "GF",
  "chatPrefix": "@gf",
  "followDistance": 3.0,
  "teleportDistance": 24.0,
  "replyInVietnamese": true,
  "language": "vi",
  "aiEnabled": true,
  "maxHistory": 8,
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash",
  "skinFile": "mcgf_skin.png",
  "skinUrl": ""
}
```

`config/mcgf.json` and `config/mcgf_history.json` are git-ignored — your API key never leaves your machine.

## Multiplayer

- Works on a **Fabric 1.21.1 server**: put the mod + Fabric API in the server `mods` folder.
- **Every player must install** Fabric + the mod (custom entity, vanilla clients can't render it).
- Does **not** work on Realms or Vanilla/Paper/Spigot servers.
- OP-only commands: `apikey`, `ai`, `prefix`. Everyone gets their own pet and AI memory.

## Build from source

Push to `main` triggers the `Build mod` workflow (Gradle 8.10.2 + JDK 21); the `.jar` is published under Artifacts. Or locally:

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-<version>.jar
```

## Full test checklist

```
/gf version → current version
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
/gf minevein → wait → /gf bag → /gf give → /gf deposit (chest next to pet)
/gf chop → /gf farm (needs ripe crops near the pet)
/gf equip → /gf gear (damage/armor correct) → weapon + armor visible on body
/gf apikey <key> → @gf where am I? (it knows your position)
/gf prefix @bot → "@bot hello"
/gf config → shows settings including skin
Drop a 64x64 PNG into config/mcgf_skin.png → relog → new skin
Restart server → AI memory and pet inventory intact
```

## License

MIT — see `LICENSE`.
