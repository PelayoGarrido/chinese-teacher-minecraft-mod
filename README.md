# Chinese Teacher (Pinyin Speaker)

Client-side Fabric mod for Minecraft 1.21.1 that integrates Mandarin Chinese and Spanish language learning directly into gameplay. Displays live Hanzi, Pinyin with tone marks, and Spanish translations, with zero-latency native text-to-speech.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-52796F?style=flat-square)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.20.1-2D6A4F?style=flat-square)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-21-1B4332?style=flat-square)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-081C15?style=flat-square)](LICENSE)

---

## Features

- **In-Game HUD**: Looking at blocks, entities, or holding items displays a top-center box with Chinese characters (Hanzi), tone-marked Pinyin, and Spanish translation.
- **Inventory & Menu Translation**: Works across survival inventories, chests, the Creative menu tabs, Recipe Book, and settings menus. Hovering over items or UI buttons displays their translation.
- **Native TTS Speech**: Uses local operating system speech synthesizers (`/usr/bin/say` on macOS, Windows SAPI, eSpeak on Linux). No internet connection required, no speech lag.
- **Self-Contained Dictionary**: Built-in dataset of 3,100+ vanilla items, blocks, and entities, plus a 26,500+ character Pinyin phonetic lookup table. Does not require external resource packs or custom font overrides.
- **Search-Bar Protection**: Automatically detects when typing in search bars (Creative search, Recipe Book, Anvil) to prevent hotkey triggers while typing.

---

## Controls

| Key | Action | Context |
| :--- | :--- | :--- |
| **`V`** | Speak Mandarin Chinese (Hanzi) | Crosshair target, held item, or hovered inventory item / menu button |
| **`B`** | Speak Spanish translation | Crosshair target, held item, or hovered inventory item / menu button |
| **Mouse Hover** | Displays HUD indicator and tooltip | In-game HUD and all container/menu screens |

> Keybinds can be rebound in **Options ➔ Controls ➔ Key Binds ➔ Pinyin Speaker**.

---

## Requirements

| Component | Requirement |
| :--- | :--- |
| **Minecraft** | 1.21.1 |
| **Mod Loader** | Fabric Loader >= 0.16.x |
| **Dependency** | Fabric API |
| **Java** | Java 21 |
| **Platforms** | macOS (Apple Silicon / Intel), Windows 10/11, Linux |

Compatible with Sodium, Iris Shaders, ModMenu, and standard performance mods.

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.1.
2. Download **Fabric API** and **`chinese-teacher-minecraft-mod-1.0.0.jar`**.
3. Place both `.jar` files in your `.minecraft/mods` directory:
   - **macOS**: `~/Library/Application Support/minecraft/mods/`
   - **Windows**: `%appdata%\.minecraft\mods\`
   - **Linux**: `~/.minecraft/mods/`
4. Launch Minecraft.

---

## Building from Source

```bash
git clone https://github.com/PelayoGarrido/chinese-teacher-minecraft-mod.git
cd chinese-teacher-minecraft-mod
./gradlew build
```

The compiled mod will be located in `build/libs/`.

---

## License & Attribution

Created by **Pelayo Garrido**.

Licensed under **[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)](LICENSE)**.

- **Attribution**: You must give appropriate credit to Pelayo Garrido and provide a link to the original repository.
- **Non-Commercial**: You may not use this mod, its code, or derivative works for commercial purposes or behind paywalls.
- **ShareAlike**: Any modifications or forks must be distributed under the same license.
- **Content Creators**: Free to use in YouTube videos, streams, and educational content with attribution in the description.
