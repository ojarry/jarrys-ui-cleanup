# Jarry's UI Cleanup

A lightweight client-side Minecraft Java mod that strips the clutter out of menus and gives the game a darker, cleaner look. Every feature can be turned on or off.

The current release is **1.0.2**. The Fabric and NeoForge builds share the same mod id (`jarrys_ui_cleanup`).

- Removes Realms, Feedback, and Bug Report buttons, plus the Realms notification dot
- Tidies the pause menu and title-screen icon row
- Skips the experimental settings warning and the backup prompt when opening a world
- Removes the chat report option and can strip chat signatures, with a safe fallback on servers that require signed chat
- Replaces the red Mojang loading screen with a dark crimson background and the Jarry logo
- Bundles Jarry's Dark UI as a built-in resource pack
- Turns off telemetry, quiets some toasts, and includes a few other optional extras

If [No Chat Reports](https://modrinth.com/mod/no-chat-reports) is installed, the chat features step aside and let that mod handle it.

## Supported versions

| Loader | Minecraft versions | Notes |
| --- | --- | --- |
| Fabric | 26.3, 26.2 | Requires [Fabric API](https://modrinth.com/mod/fabric-api). Java 25. |
| NeoForge | 26.3, 26.2 | Java 25. |

This is a client-side mod. It is not needed on a dedicated server.

## Install

1. Install the loader for your edition of the game (Fabric Loader or NeoForge) that matches your Minecraft version.
2. For Fabric, also install Fabric API for that same game version.
3. Download the jar for your loader and game version and put it in the `mods` folder.

## Report bugs and get help

- **Bug reports and crashes:** open a GitHub Issue: https://github.com/ojarry/jarrys-ui-cleanup/issues
- **Questions and support:** use GitHub Discussions: https://github.com/ojarry/jarrys-ui-cleanup/discussions

Please include your Minecraft version, loader (Fabric or NeoForge), loader version, and mod version. For crashes, attach the latest log or crash report.

## Source layout

- `common/` — shared Java source, lang, icons, and mixin config
- `fabric/` and `neoforge/` — loader-specific entrypoints and metadata
- `generated/` — the built-in Dark UI resource packs for each game version
- `versions/` — Gradle projects used to build each loader and game version
- `logo/` — the project logo
- `modrinth/` — the Modrinth listing description

## License

MIT. See [LICENSE](LICENSE).
