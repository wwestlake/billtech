# AGENTS.md

This file is a lightweight, human-readable briefing for anyone picking up this repo.
Keep it short, factual, and updated as the project evolves.

## Project summary
- Mod: Bill Tech (`billtech`)
- Target: Minecraft 1.21.5, Fabric Loom
- Purpose: Tech mod focused on filling gaps and adding complementary systems for other tech mods.

## Key dependencies
- Fabric Loader >= 0.18.4
- Fabric API 0.128.2+1.21.5
- TechReborn 5.13.3 (declared in `build.gradle`)
- RebornCore 5.13.4 (declared in `build.gradle`)

## Java version
- Java 21

## Build and run (Gradle)
- `./gradlew runClient`
- `./gradlew runServer`
- `./gradlew build`
- `./gradlew clean`

Note: This is a standard Fabric Loom setup; tasks above should exist unless the Loom config changes.

## Repo layout
- Mod entrypoints: `src/main/java/com/billtech/BillTech.java`, `src/client/java/com/billtech/BillTechClient.java`
- Blocks/entities: `src/main/java/com/billtech/block`, `src/main/java/com/billtech/block/entity`
- Rendering: `src/client/java/com/billtech/client/render`
- Menus/screens: `src/main/java/com/billtech/menu`, `src/client/java/com/billtech/client/screen`
- Assets: `src/client/resources/assets/billtech`

## Tank system (current focus)
- Tank block entity: `src/main/java/com/billtech/block/entity/TankBlockEntity.java`
- Tank renderer: `src/client/java/com/billtech/client/render/TankBlockEntityRenderer.java`
- Tank controller (menu + snapshot):
  - `src/main/java/com/billtech/block/entity/TankControllerBlockEntity.java`
  - `src/main/java/com/billtech/menu/TankControllerMenu.java`

## Current status / known issues
- Fill level logic was stabilized recently.
- Ongoing issue: fluid fill level not rendering inside the tank.
- Recent fix: server -> client sync added for tank storage updates so the renderer can see fluid/amount.
  - If rendering still fails, check BE render logic or render layer/buffer usage for fluids.

## Useful metadata
- Mod ID: `billtech`
- Name: Bill Tech
- Repo: https://github.com/wwestlake/billtech
- Contact (from `fabric.mod.json`): wwestlake@lagdaemon.com

## Notes for new helpers
- If changes are in rendering, always verify both client NBT updates and render layer/buffer setup.
- If touching tank storage balancing, watch for server/client desync and oscillation.
