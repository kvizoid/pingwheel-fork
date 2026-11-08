# 🎯 Ping Wheel Fork (Fabric 26.3 / Java 25)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-blue.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.161.0+-lightgrey.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Latest Release](https://img.shields.io/badge/Release-v1.1.1-brightgreen.svg)](https://github.com/kvizoid/pingwheel-fork/releases/latest)

An enhanced community fork of the **Ping Wheel** mod for Minecraft Fabric (Java 25). Built for seamless multiplayer communication with friends across any vanilla, proxy, or modded server without requiring server-side mods or plugins.

---

## ✨ Features

- 🌐 **Global WebSocket Relay Network**:
  - Pings are automatically shared with your friends across any server or network without needing the mod or plugin installed on the server.
  - Smart singleplayer isolation (the relay automatically idles in singleplayer worlds to avoid broadcasting private coordinates).

- 📍 **Simultaneous Multi-Ping**:
  - Place and maintain multiple active ping markers simultaneously (up to 10 markers per player, configurable in settings; default: 5).
  - Automatically rotates out the oldest marker when your maximum limit is exceeded.
  - Remove specific markers by aiming at them and pressing your ping key again.

- 📋 **Right-Click Coordinate Copy (RMB to Copy)**:
  - Hover your crosshair over any ping marker in the world to highlight its coordinates and reveal the `[RMB]` prompt.
  - Right-click to instantly copy exact block coordinates `X Y Z` directly to your system clipboard, accompanied by an audio cue and HUD confirmation.
  - Safe event interception prevents accidental item usage or block interaction when clicking a ping.

- 👤 **Dynamic Self-Ping**:
  - Quick gesture activation: **Hold Crouch (Shift) + Look Down** (pitch > 50°).
  - Dedicated keybind available in Minecraft Controls options.
  - Attaches a live marker to your player model that follows your movement, broadcasting your exact coordinates, head skin, and HP to teammates.

- 👾 **Detailed Entity HUD**:
  - 2D/3D mob and player head renders.
  - Dynamic RGB health gradient transitioning smoothly from green to yellow and red as health drops.
  - Gold absorption hearts indicator (`+X` absorption HP).
  - Custom distance and exact coordinates display.

- 📏 **Adaptive Distance LOD (200 Blocks)**:
  - Within range (<= 200m): displays complete HUD with entity head, HP bar, item icons, and exact coordinates `[X Y Z]`.
  - Long distance (> 200m): automatically condenses to a clean, non-intrusive directional arrow with rounded distance to eliminate screen clutter.
  - Adjustable detail threshold slider in the mod settings.

- 🔄 **In-Game Auto-Updater**:
  - Automatic silent background check upon game launch with dual-mirror fallback (GitHub Releases CDN and dedicated gateway).
  - Seamlessly downloads and updates the `.jar` inside your active `mods/` directory.
  - In-game HUD notification: `✔ Ping Wheel updated to vX.X.X! Restart the game.`
  - Manual check button in mod settings.

- ⚙️ **Cross-Server Sync Screen (`ServerSyncScreen`)**:
  - Link different proxy domains and direct server IPs together into one unified relay channel in a single click.

---

## 🎮 Default Controls

| Action | Key / Gesture |
|---|---|
| Place / Remove Ping | **Mouse 5 (Forward)** or Mouse Wheel |
| Copy Marker Coordinates | **Right-Click (RMB)** while aiming at a ping marker |
| Self-Ping / Share Location | **Shift + Look Down** or dedicated Controls keybind |
| Open Ping Wheel Radial Menu | Hold Ping key |
| Show Ping Author Names | Hold **Tab** (Player List) |
| Open Settings Menu | `/pingwheel config` or via **Mod Menu** |

---

## 📦 Installation

1. Prerequisites:
   - **Minecraft 26.3**
   - **Fabric Loader** (0.19.5 or newer)
   - **Fabric API**
   - **Java 25**
2. Download the latest `.jar` from [Releases](https://github.com/kvizoid/pingwheel-fork/releases/latest).
3. Drop the file into your `.minecraft/mods/` folder (or your Prism Launcher / Modrinth instance).
4. Launch Minecraft.

---

## 🛠️ Building from Source

```bash
git clone https://github.com/kvizoid/pingwheel-fork.git
cd pingwheel-fork
./gradlew build
```

Compiled mod binary: `fabric/build/libs/Ping-Wheel-1.1.1-fabric-26.3.jar`.

---

## 📄 Credits & License

- Distributed under the permissive **MIT License** (see [LICENSE](LICENSE)).
- Original Ping Wheel mod created by **[LukenSkyne (Luken)](https://github.com/LukenSkyne/Minecraft-Ping-Wheel)** with contributions by **RXJpaw**.
- This project is an independent community fork expanding multiplayer coordination, interface ergonomics, and network synchronization.
