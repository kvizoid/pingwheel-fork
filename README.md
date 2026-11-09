# ◆ Ping Wheel Fork

> Client-side tactical pinging, 3D measurement ruler, and mob threat radar for Minecraft **26.3** (Fabric).  
> Works out-of-the-box on **any** server without server mods.

---

### ✦ Features

- **🌐 Cross-Server Relay** `▸` Built-in WebSocket relay lets you and friends share pings anywhere (Hypixel, SMPs, LAN) without server-side mods.
- **📏 3D Ruler** `▸` Hold `Shift` to dynamically link active pings into a measuring tape (`4 б`, `3 × 5 б`). `Shift + RMB` locks ruler mode permanently. Fixed 3D midpoint anchoring.
- **⚠️ Mob Threat Radar** `▸` Minimalist 1px dashed danger rings on the floor under hostile mobs (Creeper 3.2m, Skeleton 15m, Warden 16m, etc.). Does not clip through walls or mobs.
- **🎲 Slot Animations** `▸` Mouse wheel scrolls coordinates between Overworld and Nether with a vertical roulette roll and bounce settle. Pop-in scale bounce on ping placement.
- **📦 Container Memory** `▸` Hovering over a pinged chest, barrel, or shulker box displays a snapshot of its stored items.
- **📋 Quick Clipboard** `▸` Look at any mark and press `RMB` to copy its exact `X Y Z` coordinates.
- **🔄 Auto-Updater** `▸` Silent update checks via GitHub Releases on startup with in-place replacement.

---

### ⚡ Controls

| Key | Action |
|---|---|
| `Mouse 5` | Place / remove ping |
| `Hold Mouse 5` | Radial ping wheel |
| `RMB` on ping | Copy coordinates to clipboard |
| `Scroll Wheel` on ping | Toggle Overworld ⇄ Nether coordinates |
| `Shift` (held) | Dynamic 3D ruler between marks |
| `Shift + RMB` on ping | Toggle permanent ruler mode |
| `Shift + Look Down` | Self-ping (shares skin head, HP, and position) |
| `Hold Tab` | Show player names and distance labels |
| `/pingwheel config` | Open in-game settings |

---

### 📦 Setup

1. Install **Fabric Loader** `0.19.5+` + **Fabric API** for Minecraft **26.3** (Java 25).
2. Grab the latest `.jar` from [Releases](https://github.com/kvizoid/pingwheel-fork/releases/latest).
3. Drop into `.minecraft/mods`.

---

### ◈ License & Credits

- Fork maintained by **kvizoid**.
- Original mod by **[LukenSkyne](https://github.com/LukenSkyne/Minecraft-Ping-Wheel)** & **RXJpaw** under [MIT License](LICENSE).
