# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build          # Build the mod
./gradlew runClient      # Launch Minecraft client with the mod
./gradlew runServer      # Launch dedicated server
./gradlew runData        # Run data generation (outputs to src/generated/resources/)
./gradlew runGameTestServer  # Run game tests
```

Gradle daemon, parallel builds, caching, and configuration cache are all enabled in `gradle.properties`. The mod uses **NeoForge 21.11.38-beta** for **Minecraft 1.21.11** with **Java 21** and **Parchment mappings (2025.12.20)**.

## Architecture

This is a NeoForge mod (modid: `blockrooms`, group: `name.blockrooms`). All registration uses the NeoForge `DeferredRegister` pattern — blocks, items, entities, sound events, data components, creative tabs, recipe types, and chunk generators are each registered in their own `Mod*` class and wired in `Blockrooms.java`'s constructor.

### Entry Points

- **`Blockrooms`** — `@Mod`-annotated common entry. Registers all components on the mod event bus, then registers event handlers (`RubyTransHandler`, `NoclipHandler`, `BlockLevel4Handler`) on the NeoForge (game) event bus.
- **`BlockroomsClient`** — `@Mod(dist=CLIENT)` client-only entry. Registers entity renderers and config screen extension point.
- **`Config`** — Single config value `ruby_trans_time` (ticks, default 1200 = 60s). Uses NeoForge's `ModConfigSpec`.

### Custom Dimensions

Two custom dimensions defined as `ResourceKey<Level>` constants in `ModLevels`:

| Key | Generator | Description |
|---|---|---|
| `blocklevel0` | `BlockLevel0Generator` (flat procedural) | 5-block-tall ceiling room: oak planks + brown carpet floor, stone ceiling, patterned redstone lamp lighting. Uses `fixed` biome source. |
| `blocklevel4` | `BlockLevel4Generator` (extends `NoiseBasedChunkGenerator`) | Overworld-like terrain with bedrock floor raised to y=-50 and suppressed carvers. Uses `multi_noise` biome source. |

Dimension JSON lives in `data/blockrooms/dimension/` and `data/blockrooms/dimension_type/`.

### Dimension Travel (Noclip)

`NoclipHandler` uses a `FlexibleMap<ResourceKey<Level>, BlockState, destination>` to route players between dimensions. On suffocation damage (`inWall`), it checks the player's current dimension + the block they're inside, and teleports them with probability `chance`. Static initializer defines the routes: Overworld → Blocklevel0 (20% on any block), Overworld + amethyst → Nether (90%), Blocklevel0 + amethyst → Blocklevel4 (90%).

Players can also trigger noclip manually by pressing **N** (`ModKeyBindings` → `NoclipPayload` sent to server).

### Quartz Elevator

`QuartzElevatorBlock` emits reverse-portal particles. When a player stands on it and presses **Jump** or **Shift**, `ModKeyHandler` sends an `ElevatorTeleportPayload`; the server searches up to 16 blocks in that direction for another quartz elevator block with safe air above it, then teleports the player.

### GunBow

`GunBowItem` auto-consumes random items from the player's inventory every 20 ticks (15% chance). On right-click, it shoots the first accumulated item as a projectile. The projectile factory (`createProjectileForAmmo`) dispatches on item type: arrows become `AbstractArrow`, eggs spawn chickens, TNT/minecart spawn their respective entities, blocks become `BlockProjectile`, everything else becomes `ItemProjectile` (with subclass dispatch for discs and tools).

### Projectile Hierarchy

`ArrowLikeProjectile` (abstract base) → `ItemProjectile` / `BlockProjectile`. Both carry their payload as synced entity data. `ItemProjectile.of()` factory further dispatches to `DiscProjectile` or `ToolItemProjectile` based on data components. On impact, items drop as `ItemEntity`, blocks spawn as `FallingBlockEntity`. Custom renderers (`BlockProjectileRenderer`, `ItemProjectileRenderer`) use dedicated render state records.

### BlockLevel4 Mechanics

`BlockLevel4Handler` enforces:
- **Custom block drops**: 40% chance to replace drops with loot from `blocklevel4_drop` loot table (skipped if tool has Silk Touch).
- **Monster night immunity**: Monsters take 0 damage during daytime (13000–24000 ticks), unless the attacker is a creative-mode player.
- **Infinite poison**: Any poison effect applied to a player becomes infinite duration.
- **Poison cleanse on leave**: Poison is removed when any living entity leaves the dimension.

### Dynamic Lighting

`DynamicLightingHandler` (client-side) provides a flood-fill light when a player holds glowstone dust in either hand. Two mixins inject it: `LevelRendererMixin` (block rendering) and `EntityRendererMixin` (entity rendering). Light starts at level 10 at the player's eye position and decays by 1 per block, respecting face occlusion.

### Error Crafting

`ErrorCraftingMenu` + `ErrorCraftingRecipe` provide a custom crafting table with its own recipe type (`ModRecipeTypes.ERROR_CRAFTING`). `ResultSlotMixin` intercepts `getRemainingItems` to check error recipes first, then fall back to vanilla. Taking a result has a 5% chance of inflicting Nausea. `RecipeCraftingHolderMixin` prevents unlocking error-crafting recipes in the recipe book.

### Mixins

All declared in `src/main/resources/blockrooms.mixins.json`:

| Mixin | Target | Purpose |
|---|---|---|
| `ChunkGeneratorMixin` | `ChunkGenerator` | Disable amethyst geodes and lava lakes in blocklevel4 |
| `StructureManagerMixin` | `StructureManager` | Restrict structures to villages only in blocklevel4 |
| `RecipeCraftingHolderMixin` | `RecipeCraftingHolder` | Suppress error-crafting recipe unlock |
| `ResultSlotMixin` | `ResultSlot` | Custom remaining-items logic + nausea on error craft |
| `ResultSlotInvoker` | `ResultSlot` | Accessor interface for `copyAllInputItems` |
| `DebugScreenOverlayMixin` | `DebugScreenOverlay` | Custom debug text in mod dimensions |
| `EntityRendererMixin` (client) | `EntityRenderer` | Inject dynamic lighting into entity brightness |
| `LevelRendererMixin` (client) | `LevelRenderer` | Inject dynamic lighting into block brightness |

### Data Generation

`ModModelProvider` in `datagen/` handles model generation. Run with `./gradlew runData`; output goes to `src/generated/resources/`. Static data (dimensions, dimension types, biomes, loot tables, recipes, tags) lives in `src/main/resources/data/`.

### Network Protocol

Payloads use NeoForge's `CustomPacketPayload` with `STREAM_CODEC`. Both are `playToServer`:
- `NoclipPayload` (empty, unit codec) — triggers noclip teleport
- `ElevatorTeleportPayload` (boolean `upwards`) — triggers elevator teleport
