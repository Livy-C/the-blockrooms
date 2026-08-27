package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.concurrent.CompletableFuture;

public class BlockLevel303Generator extends BaseBlockLevelGenerator {

    public static final int GROUND_Y = 1;
    public static final int LANE_GAP = 32;
    public static final int LANE_HALF = 3;
    public static final int SEA_LANTERN_INTERVAL = 6;
    public static final int SUBURB_UNIT_CHUNKS = 16;
    public static final int CITY_MIN_FLOORS = 3;
    public static final int CITY_MAX_FLOORS = 7;
    public static final int FLOOR_HEIGHT = 5;
    public static final int SPAWN_FLOORS = 5;
    public static final int SPAWN_BUILDING_MIN = 4;
    public static final int SPAWN_BUILDING_MAX = 28;
    private static final int SUBURB_AT_ORIGIN = 294;
    private static final int SUBURB_AT_FAR = 800;
    private static final int SUBURB_FAR_DIST = 10000;

    public static final MapCodec<BlockLevel303Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel303Generator::getBiomeSource)
            ).apply(instance, BlockLevel303Generator::new)
    );

    public BlockLevel303Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }


    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getMinBlockX() + x;
                int wz = cp.getMinBlockZ() + z;
                if (isVoid(wx, wz)) {
                    continue;
                }
                if (inSpawnBuilding(wx, wz)) {
                    for (int y = -5; y <= 0; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z),
                                y == 0 ? Blocks.STONE.defaultBlockState()
                                        : y == -5 ? Blocks.SMOOTH_STONE.defaultBlockState()
                                        : Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_NONE);
                    }
                } else {
                    chunk.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }


    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();
        boolean suburbChunk = isSuburbChunk(seed, cp.x, cp.z);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getMinBlockX() + x;
                int wz = cp.getMinBlockZ() + z;
                if (isVoid(wx, wz)) {
                    continue;
                }
                BlockState ground;
                if (inSpawnBuilding(wx, wz)) {
                    ground = Blocks.QUARTZ_BLOCK.defaultBlockState();
                } else if (suburbChunk) {
                    ground = Blocks.GRASS_BLOCK.defaultBlockState();
                } else if (inLane(wx) || inLane(wz)) {
                    ground = Blocks.SMOOTH_STONE.defaultBlockState();
                } else {
                    ground = floorMaterial(seed, wx, wz);
                }
                level.setBlock(new BlockPos(wx, GROUND_Y, wz), ground, Block.UPDATE_NONE);
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getMinBlockX() + x;
                int wz = cp.getMinBlockZ() + z;
                if (isVoid(wx, wz)) {
                    continue;
                }
                if (inSpawnBuilding(wx, wz)) {
                    placeSpawnBuildingPart(level, wx, wz, seed);
                } else if (suburbChunk) {
                    placeSuburbPart(level, wx, wz, seed);
                } else if (inLane(wx) || inLane(wz)) {
                    placeLanePart(level, wx, wz, seed);
                } else {
                    placeCityPart(level, wx, wz, seed);
                }
            }
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
    }


    public static boolean isVoid(int wx, int wz) {
        return wx < 0 || wz < 0;
    }

    public static boolean inLane(int v) {
        return Math.abs(relToLane(v)) <= LANE_HALF;
    }

    public static int relToLane(int v) {
        int center = Math.floorDiv(v + LANE_GAP / 2, LANE_GAP) * LANE_GAP;
        return v - center;
    }

    public static boolean inSpawnBuilding(int wx, int wz) {
        return wx >= SPAWN_BUILDING_MIN && wx <= SPAWN_BUILDING_MAX
                && wz >= SPAWN_BUILDING_MIN && wz <= SPAWN_BUILDING_MAX;
    }

    public static boolean isSuburbChunk(long seed, int chunkX, int chunkZ) {
        int unitX = Math.floorDiv(chunkX, SUBURB_UNIT_CHUNKS);
        int unitZ = Math.floorDiv(chunkZ, SUBURB_UNIT_CHUNKS);
        if (unitX >= -1 && unitX <= 0 && unitZ >= -1 && unitZ <= 0) {
            return false;
        }
        int unitSize = SUBURB_UNIT_CHUNKS * 16;
        double d = Math.hypot(unitX * unitSize + unitSize / 2.0, unitZ * unitSize + unitSize / 2.0);
        double chance = SUBURB_AT_ORIGIN / 1000.0
                + (SUBURB_AT_FAR - SUBURB_AT_ORIGIN) / 1000.0 * Math.min(1.0, d / SUBURB_FAR_DIST);
        return hash(seed, unitX, unitZ, 0x30301L) % 1000 < (int) (chance * 1000);
    }

    private static int cellIndex(int v) {
        return Math.floorDiv(v + LANE_GAP / 2, LANE_GAP);
    }

    private static BlockState floorMaterial(long seed, int wx, int wz) {
        return switch ((int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30302L) % 4)) {
            case 0 -> Blocks.OAK_PLANKS.defaultBlockState();
            case 1 -> Blocks.QUARTZ_BLOCK.defaultBlockState();
            case 2 -> Blocks.SMOOTH_STONE.defaultBlockState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        };
    }

    public static int buildingFloors(long seed, int wx, int wz) {
        return CITY_MIN_FLOORS + (int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30303L)
                % (CITY_MAX_FLOORS - CITY_MIN_FLOORS + 1));
    }


    private static void placeLanePart(WorldGenLevel level, int wx, int wz, long seed) {
        int rx = relToLane(wx);
        int rz = relToLane(wz);
        boolean xLane = Math.abs(rz) <= LANE_HALF;
        boolean zLane = Math.abs(rx) <= LANE_HALF;
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        if (xLane && zLane) {
            if (Math.abs(rx) == LANE_HALF || Math.abs(rz) == LANE_HALF) {
                level.setBlock(p, Blocks.SANDSTONE_SLAB.defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (xLane) {
            if (Math.abs(rz) == LANE_HALF) {
                level.setBlock(p, Blocks.SANDSTONE_SLAB.defaultBlockState(), Block.UPDATE_NONE);
            } else if (rz == 0 && Math.floorMod(wx, SEA_LANTERN_INTERVAL) == 0) {
                level.setBlock(p, Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_NONE);
            }
        } else {
            if (Math.abs(rx) == LANE_HALF) {
                level.setBlock(p, Blocks.SANDSTONE_SLAB.defaultBlockState(), Block.UPDATE_NONE);
            } else if (rx == 0 && Math.floorMod(wz, SEA_LANTERN_INTERVAL) == 0) {
                level.setBlock(p, Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_NONE);
            }
        }
    }


    private static void placeCityPart(WorldGenLevel level, int wx, int wz, long seed) {
        int floors = buildingFloors(seed, wx, wz);
        int h = floors * FLOOR_HEIGHT;
        int rx = relToLane(wx);
        int rz = relToLane(wz);
        boolean edgeX = Math.abs(rx) == LANE_HALF + 1;
        boolean edgeZ = Math.abs(rz) == LANE_HALF + 1;
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        if (edgeX || edgeZ) {
            for (int y = 1; y <= h; y++) {
                if (y == 1 && isDoorGap(wx, wz)) {
                    continue;
                }
                BlockState wall = (y % 2 == 0) ? Blocks.GLASS.defaultBlockState() : wallMaterial(seed, wx, wz);
                level.setBlock(new BlockPos(wx, y, wz), wall, Block.UPDATE_NONE);
            }
        } else if (hash(seed, wx, wz, 0x30304L) % 200 == 0) {
            BlockState facility = switch ((int) (hash(seed, wx, wz, 0x30305L) % 3)) {
                case 0 -> Blocks.CRAFTING_TABLE.defaultBlockState();
                case 1 -> Blocks.FURNACE.defaultBlockState();
                default -> Blocks.ENCHANTING_TABLE.defaultBlockState();
            };
            level.setBlock(p, facility, Block.UPDATE_NONE);
        }
        level.setBlock(new BlockPos(wx, h + 1, wz), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Block.UPDATE_NONE);
    }

    private static BlockState wallMaterial(long seed, int wx, int wz) {
        return switch ((int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30306L) % 4)) {
            case 0 -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case 1 -> Blocks.YELLOW_CONCRETE.defaultBlockState();
            case 2 -> Blocks.RED_CONCRETE.defaultBlockState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        };
    }

    private static boolean isDoorGap(int wx, int wz) {
        int rx = relToLane(wx);
        int rz = relToLane(wz);
        if (Math.abs(rx) == LANE_HALF + 1) {
            int m = Math.floorMod(rz, 12);
            return m == 0 || m == 1;
        }
        if (Math.abs(rz) == LANE_HALF + 1) {
            int m = Math.floorMod(rx, 12);
            return m == 0 || m == 1;
        }
        return false;
    }


    private static final ResourceKey<ConfiguredFeature<?, ?>> CF_OAK =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace("oak"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> CF_BIRCH =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace("birch"));

    private static void placeSuburbPart(WorldGenLevel level, int wx, int wz, long seed) {
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        if (hash(seed, Math.floorDiv(wx, 8), Math.floorDiv(wz, 8), 0x30307L) % 3 == 0) {
            boolean carrot = hash(seed, wx, wz, 0x30308L) % 2 == 0;
            level.setBlock(p, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_NONE);
            level.setBlock(new BlockPos(wx, GROUND_Y + 1, wz),
                    (carrot ? Blocks.CARROTS : Blocks.WHEAT).defaultBlockState(), Block.UPDATE_NONE);
            return;
        }
        if (hash(seed, wx, wz, 0x30309L) % 30 == 0) {
            boolean birch = hash(seed, wx, wz, 0x3030AL) % 2 == 0;
            var registry = level.getLevel().registryAccess().lookup(Registries.CONFIGURED_FEATURE).orElseThrow();
            registry.get(birch ? CF_BIRCH : CF_OAK).orElseThrow().value()
                    .place(level, level.getLevel().getChunkSource().getGenerator(), level.getRandom(), p);
        }
    }


    private static void placeSpawnBuildingPart(WorldGenLevel level, int wx, int wz, long seed) {
        int ix = wx - SPAWN_BUILDING_MIN;
        int iz = wz - SPAWN_BUILDING_MIN;
        int top = SPAWN_FLOORS * FLOOR_HEIGHT; // 25
        boolean edgeX = ix == 0 || ix == 24;
        boolean edgeZ = iz == 0 || iz == 24;
        boolean shaft = ix >= 3 && ix <= 4 && iz >= 3 && iz <= 4;
        boolean elevatorColumn = ix == 2 && iz == 3;
        boolean elevatorPad = ix == 2 && iz == 4;
        boolean stairwell = ix == 3 && iz >= 5 && iz <= 9;

        if (edgeX || edgeZ) {
            for (int y = 1; y <= top; y++) {
                if (y == 1 && isSpawnDoor(wx, wz)) {
                    continue;
                }
                boolean pillar = (ix % 4 == 0 && iz % 4 == 0);
                level.setBlock(new BlockPos(wx, y, wz),
                        pillar ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState() : Blocks.BLUE_STAINED_GLASS.defaultBlockState(),
                        Block.UPDATE_NONE);
            }
            return;
        }
        if (shaft) {
            for (int y = 1; y <= top; y++) {
                level.setBlock(new BlockPos(wx, y, wz), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (elevatorColumn) {
            for (int y = 1; y <= top; y++) {
                level.setBlock(new BlockPos(wx, y, wz), Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (elevatorPad) {
            for (int floor = 0; floor < SPAWN_FLOORS; floor++) {
                int y = 1 + floor * FLOOR_HEIGHT;
                level.setBlock(new BlockPos(wx, y, wz), ModBlocks.QUARTZ_ELEVATOR.get().defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (stairwell) {
            for (int floor = 0; floor < SPAWN_FLOORS - 1; floor++) {
                int base = 1 + floor * FLOOR_HEIGHT;
                int step = 9 - iz;
                int y = base + step;
                if (y >= base + 1 && y <= base + FLOOR_HEIGHT) {
                    level.setBlock(new BlockPos(wx, y, wz), Blocks.QUARTZ_STAIRS.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
            return;
        }
        for (int floor = 0; floor < SPAWN_FLOORS; floor++) {
            int y = 1 + floor * FLOOR_HEIGHT;
            level.setBlock(new BlockPos(wx, y, wz),
                    (floor % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.OAK_PLANKS).defaultBlockState(), Block.UPDATE_NONE);
        }
        if (hash(seed, wx, wz, 0x3030CL) % 50 == 0) {
            level.setBlock(new BlockPos(wx, GROUND_Y, wz), Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
        }
    }

    private static boolean isSpawnDoor(int wx, int wz) {
        if (wz == SPAWN_BUILDING_MAX && (wx == 10 || wx == 11)) return true;
        if (wz == SPAWN_BUILDING_MIN && (wx == 20 || wx == 21)) return true;
        if (wx == SPAWN_BUILDING_MIN && (wz == 16 || wz == 17)) return true;
        return wx == SPAWN_BUILDING_MAX && (wz == 8 || wz == 9);
    }


    private static long hash(long seed, int a, int b, long salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xBF58476D1CE4E5B9L) ^ salt;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}