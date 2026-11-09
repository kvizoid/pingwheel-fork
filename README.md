<div align="center">
  <img src=".github/icon.png" width="128" height="128" alt="Ping Wheel Logo" />
  <h1>Ping Wheel Fork</h1>
  <p>Client-side tactical pinging, 3D measurement ruler, and mob threat radar for Minecraft 26.3 (Fabric).</p>
  <p>Works out of the box on any server without server-side mods.</p>
</div>

---

### Features

- **Cross-Server Relay** - Built-in WebSocket relay lets you and your friends share pings across any server or LAN without server mods.
- **3D Ruler** - Hold `Shift` to dynamically link active pings into a measuring tape (`4 б`, `3 × 5 б`). `Shift + RMB` locks ruler mode.
- **Mob Threat Radar** - Minimalist 1px dashed danger rings under hostile mobs (Creeper 3.2m, Skeleton 15m, Warden 16m). Occluded by terrain and entity hitboxes.
- **Slot Animations** - Smooth roulette roll animation when scrolling between Overworld and Nether coordinates. Pop-in scale bounce on ping placement.
- **Persistent Container Memory** - Hovering over a pinged chest, barrel, or shulker box displays a snapshot of its stored items, remembered across reconnects and dimensions.
- **Distance Protection** - Beyond 200 blocks, pings show only distance without exposing player names, health, or coordinates.
- **Quick Clipboard** - Aim at any mark within 200 blocks and press `RMB` to copy its exact coordinates to clipboard.
- **Auto-Updater** - Automatic update checks via GitHub Releases on startup with one-click in-place update.

---

### Controls

| Key | Action |
|---|---|
| `Mouse 5` | Place / remove ping |
| `Hold Mouse 5` | Radial ping wheel |
| `RMB` on ping | Copy coordinates to clipboard (within 200m) |
| `Scroll Wheel` on ping | Toggle Overworld / Nether coordinates |
| `Shift` (held) | Dynamic 3D ruler between marks |
| `Shift + RMB` on ping | Toggle permanent ruler mode |
| `Shift + Look Down` | Self-ping (shares head skin, HP, and position) |
| `Hold Tab` | Show player names and distance labels |
| `/pingwheel config` | Open in-game settings |

---

### Setup

1. Install Fabric Loader 0.19.5+ and Fabric API for Minecraft 26.3 (Java 25).
2. Download the latest `.jar` from [Releases](https://github.com/kvizoid/pingwheel-fork/releases/latest).
3. Place into `.minecraft/mods`.

---

### License & Credits

- Fork maintained by **kvizoid**.
- Original mod by **[LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel)** & **RXJpaw** under [MIT License](LICENSE).
