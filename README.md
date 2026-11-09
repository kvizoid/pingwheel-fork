# Ping Wheel (Fork)

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-informational?style=flat-square&color=4CAF50)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.5+-informational?style=flat-square&color=E8D5B5)](https://fabricmc.net/)
[![Java 25](https://img.shields.io/badge/Java-25-informational?style=flat-square&color=ED8B00)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Latest Release](https://img.shields.io/github/v/release/kvizoid/pingwheel-fork?style=flat-square&color=38BDF8)](https://github.com/kvizoid/pingwheel-fork/releases/latest)

An enhanced client-first fork of [Ping Wheel by LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel). It allows you to place visual marks in the 3D world, measure building dimensions, check mob danger zones, and share pings with teammates on **any** Minecraft server without requiring server mods or plugins.

---

## Key Features

### 🌐 Cross-Server WebSocket Relay
- Pings are synchronized across teammates via a lightweight, secure WebSocket relay (`relay.bubuntu.tech`).
- Works on vanilla servers, Hypixel, private SMPs, proxies, and LAN.
- Channel-based isolation: only players using the same channel see each other's pings.
- Optional server synchronization whitelisting via `/pingwheel config`.

### 📏 3D Ruler (Рулетка)
- Dynamic linking: holding `Shift` links all active marks with a dashed line and calculates block dimensions in real-time.
- Permanent mode: hover over an existing mark and press `Shift + RMB` to toggle ruler mode permanently.
- Inclusive block measurement: displays exact block spans (`4 б`, `5 × 8 б`) without clutter.
- True 3D projection: midpoint badge is firmly anchored in 3D world space and does not drift when rotating the camera.
- Translucent, distraction-free aesthetic matching standard ping markers.

### ⚠️ Mob Threat Radii (Danger Circles)
- Pinging a hostile mob displays a subtle 1px dashed danger circle on the ground showing its aggro / attack range.
- Supported mobs: Creeper (3.2m), Skeleton / Stray / Bogged (15m), Zombie / Piglin Brute / Vindicator (2.5m), Warden (16m), Witch (9m), Spider (5m), Ghast (24m), Blaze (16m).
- Intelligent occlusion: the circle is culled if occluded by the mob itself or solid terrain, preventing it from clipping through the mob or walls.

### 🎲 Tactile Micro-Animations
- **Roulette Dimension Switch**: scrolling the mouse wheel on a hovered ping smoothly animates coordinates between the Overworld and Nether with a slot-machine roll (up or down based on wheel direction) and a gentle settling bounce.
- **Spawn Pop-in**: newly placed marks rapidly scale up from zero with a punchy overshoot and settle (260 ms).

### 📦 Container Memory
- Opening any chest, barrel, or shulker box caches its inventory snapshot.
- Pinging that container afterwards renders a preview of the stored items and item counts.

### 🧭 Multi-Ping & Quick Clipboard
- Place up to 10 simultaneous marks (configurable).
- Aim at any ping and press **RMB** to copy its exact coordinates straight to the clipboard.
- Dimension conversion: coordinates convert between Overworld and Nether (8:1 ratio) on scroll.
- Self-ping: crouch (`Shift`) and look straight down to share your location, skin head, and current health with teammates.

### 🔄 Integrated Auto-Updater
- Checks for new versions on startup and automatically updates the jar in your `.minecraft/mods` folder.
- Manual check button available directly in the `/pingwheel config` settings menu.

---

## Keybinds & Controls

| Action | Default Input |
|---|---|
| **Place / Remove Ping** | `Mouse 5` (Thumb Forward) |
| **Ping Wheel Radial Menu** | Hold `Mouse 5` |
| **Copy Coords to Clipboard** | Aim at ping + `Right-Click (RMB)` |
| **Convert Coords (Nether / Overworld)** | Aim at ping + `Mouse Scroll Up / Down` |
| **Dynamic Ruler Link** | Hold `Shift` (with 2+ marks active) |
| **Toggle Permanent Ruler** | Aim at ping + `Shift + Right-Click` |
| **Self-Ping (Share HP & Head)** | `Shift` + Look straight down |
| **Show Player Names** | Hold `Tab` (Player List) |
| **Open Settings Menu** | `/pingwheel config` or Mod Menu |

---

## Installation & Requirements

1. Ensure **Fabric Loader** (`0.19.5`+) and **Fabric API** are installed.
2. Requires **Java 25** and Minecraft **26.3**.
3. Download the latest jar from [Releases](https://github.com/kvizoid/pingwheel-fork/releases/latest) and place it in your `mods` folder.

---

## Configuration

Open the in-game settings screen via `/pingwheel config` or through Mod Menu:
- **Ping Channel**: Set a custom channel name to group with friends.
- **Server Sync**: Whitelist servers or enable global cross-server relay.
- **3D Ruler**: Toggle ruler line and badge rendering.
- **Mob Threat Radius**: Toggle danger circles under hostile mobs.
- **Detail Distance**: Distance threshold before distant pings switch to minimalist arrows.
- **Max Simultaneous Pings**: Limit concurrent active pings per player (1 to 10).

---

## Credits & License

- Original mod created by **[LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel)** and **RXJpaw**.
- Distributed under the [MIT License](LICENSE).
