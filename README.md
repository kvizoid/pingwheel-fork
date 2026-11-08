# Ping Wheel (Fork)

A Minecraft Fabric mod for marking locations and communicating with friends, based on the original [Ping Wheel by LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel).

This fork adds built-in relay support (so you can see each other's pings on any server without needing server-side mods), multi-ping, quick coordinate copying, entity info, and background updates.

## What's new in this fork

- **Works on any server (Built-in Relay)**: You and your friends can see each other's pings anywhere (hypixel, vanilla servers, LAN, private servers). No server mod or plugins required.
- **Multiple pings**: Ping up to 10 locations at the same time (configurable, default is 5). Oldest pings get replaced automatically.
- **Right-click to copy coords**: Aim at any ping in the world and right-click to copy its `X Y Z` coordinates straight to your clipboard.
- **Ping yourself**: Crouch (Shift) and look down, or press the self-ping keybind. Shows your current location, head, and health to friends.
- **Entity info & health**: Pinging a player or mob displays their head, name, distance, coordinates, and a color-coded health bar with absorption.
- **Clean distance scaling**: Nearby pings show full details; distant pings (>200 blocks) switch to a clean distance arrow so your screen doesn't get cluttered.
- **Auto-updater**: Checks for updates when the game launches and updates the jar in your `mods` folder automatically.
- **Server sync settings**: In-game menu (`/pingwheel config`) to link different server addresses or proxies to the same channel.

## Keybinds

| Key | Action |
|---|---|
| **Mouse 5** (Forward) | Place or remove a ping |
| **Right-Click** | Copy coordinates while aiming at a ping |
| **Shift + Look Down** | Ping yourself / share current location |
| **Hold Ping Key** | Open the ping wheel menu |
| **Hold Tab** | Show player names on pings |

Configure keys and options via `/pingwheel config` or through Mod Menu.

## Requirements

- Minecraft **26.3**
- Fabric Loader **0.19.5+**
- Fabric API
- Java 25

## Credits

- Original mod by **[LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel)** and **RXJpaw** (licensed under MIT).
