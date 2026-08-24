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

/**
 * BlockLevel 303「浮云一梦之城」生成器。
 *
 * <p>世界 = 无尽混凝土城市 + 郊区：
 * <ul>
 *   <li><b>西北虚空</b>：x&lt;0 或 z&lt;0 的区域不生成地面（蓝色虚空），跳入由 BlockLevel303Handler 处理；</li>
 *   <li><b>区域划分</b>：先按 <b>16×16 区块（256×256 格）</b>大单元整体划分城市/郊区
 *       （离原点越远郊区概率越大，原点约 29% → 10000 格处约 80%），单元内统一生成、互不混合；
 *       出生点附近单元强制城市；</li>
 *   <li><b>城市</b>：每 {@link #LANE_GAP} 格一条 <b>6 格宽小路</b>（4 格路面 + 两侧各 1 格砂岩台阶），
 *       沿路每 {@link #SEA_LANTERN_INTERVAL} 格一个海晶灯；巷间区域由<b>大楼填满</b>：
 *       每栋楼（巷间单元）在生成前按哈希抽好<b>统一墙色</b>，层数 3~7 层、<b>每层 5 格高</b>，
 *       玻璃窗带、每 12 格 2 宽门洞、哈希地板材质、工作台/熔炉/附魔台设施、天花板；</li>
 *   <li><b>郊区</b>：草地 + 原版橡树/白桦（configured feature）+ 成片农田；</li>
 *   <li><b>出生大楼</b>：第一个楼区单元（x,z ∈ [4,28]），蓝色染色玻璃 + 浅蓝混凝土柱外壳，
 *       <b>5 层 × 每层 5 格</b>：楼层地板、铁块电梯柱 + 石英电梯方块、石英台阶楼梯间、楼梯井；
 *       地下挖空（y&lt;0 触发回退传送）。
 *   </li>
 * </ul>
 */
public class BlockLevel303Generator extends BaseBlockLevelGenerator {

    public static final int GROUND_Y = 1;
    /** 巷间距（格）：每 32 格一条小路 */
    public static final int LANE_GAP = 32;
    /** 巷带半宽：总宽 6（4 格路面 + 两侧各 1 格台阶） */
    public static final int LANE_HALF = 3;
    /** 沿路每 6 格一个海晶灯 */
    public static final int SEA_LANTERN_INTERVAL = 6;
    /** 城市/郊区划分单元：16×16 区块（256×256 格） */
    public static final int SUBURB_UNIT_CHUNKS = 16;
    /** 大楼楼层数范围 */
    public static final int CITY_MIN_FLOORS = 3;
    public static final int CITY_MAX_FLOORS = 7;
    /** 每层高度（格） */
    public static final int FLOOR_HEIGHT = 5;
    /** 出生大楼层数 */
    public static final int SPAWN_FLOORS = 5;
    /** 出生大楼范围（第一个楼区单元，含边界） */
    public static final int SPAWN_BUILDING_MIN = 4;
    public static final int SPAWN_BUILDING_MAX = 28;
    /** 郊区占比（千分比）：原点约 29.4%（12:5），10000 格处 80% */
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

    // ---------- 地基（SPAWN 阶段，ChunkAccess 直写；地面方块在 FEATURES 阶段铺设） ----------

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getMinBlockX() + x;
                int wz = cp.getMinBlockZ() + z;
                if (isVoid(wx, wz)) {
                    continue; // 西北虚空：不生成地面
                }
                if (inSpawnBuilding(wx, wz)) {
                    // 出生大楼：y0 石头基底、y-5 平滑石、中间挖空（地下层）
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

    // ---------- 地面 + 装饰（FEATURES 阶段） ----------

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();
        boolean suburbChunk = isSuburbChunk(seed, cp.x, cp.z);
        // 先铺 y=1 地面（fillFromNoise 只铺了 y=0 地基；郊区/城市按区块整体划分，不逐格混合）
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
        // 装饰（郊区区块：树/农田；城市区块：巷带/大楼/设施等）
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

    // ---------- 判定工具 ----------

    /** 西北虚空：x<0 或 z<0 */
    public static boolean isVoid(int wx, int wz) {
        return wx < 0 || wz < 0;
    }

    /** 该坐标是否在巷带内（沿某方向的路） */
    public static boolean inLane(int v) {
        return Math.abs(relToLane(v)) <= LANE_HALF;
    }

    /** 相对最近巷中心的位置（-LANE_GAP/2 .. LANE_GAP/2） */
    public static int relToLane(int v) {
        int center = Math.floorDiv(v + LANE_GAP / 2, LANE_GAP) * LANE_GAP;
        return v - center;
    }

    /** 出生大楼范围（第一个楼区单元） */
    public static boolean inSpawnBuilding(int wx, int wz) {
        return wx >= SPAWN_BUILDING_MIN && wx <= SPAWN_BUILDING_MAX
                && wz >= SPAWN_BUILDING_MIN && wz <= SPAWN_BUILDING_MAX;
    }

    /**
     * 按 16×16 区块大单元划分郊区：整个单元整体判定（先划区域再生成，不与城市逐格混合）。
     * 离原点越远郊区概率越大（原点约 29% → 10000 格处约 80%）；出生点附近单元强制城市。
     */
    public static boolean isSuburbChunk(long seed, int chunkX, int chunkZ) {
        int unitX = Math.floorDiv(chunkX, SUBURB_UNIT_CHUNKS);
        int unitZ = Math.floorDiv(chunkZ, SUBURB_UNIT_CHUNKS);
        if (unitX >= -1 && unitX <= 0 && unitZ >= -1 && unitZ <= 0) {
            return false; // 出生点附近（原点 256×256 单元）强制城市
        }
        int unitSize = SUBURB_UNIT_CHUNKS * 16;
        double d = Math.hypot(unitX * unitSize + unitSize / 2.0, unitZ * unitSize + unitSize / 2.0);
        double chance = SUBURB_AT_ORIGIN / 1000.0
                + (SUBURB_AT_FAR - SUBURB_AT_ORIGIN) / 1000.0 * Math.min(1.0, d / SUBURB_FAR_DIST);
        return hash(seed, unitX, unitZ, 0x30301L) % 1000 < (int) (chance * 1000);
    }

    /** 巷间单元坐标（每栋大楼的"地块"） */
    private static int cellIndex(int v) {
        return Math.floorDiv(v + LANE_GAP / 2, LANE_GAP);
    }

    /** 城市楼区地板材质（按大楼单元哈希统一） */
    private static BlockState floorMaterial(long seed, int wx, int wz) {
        return switch ((int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30302L) % 4)) {
            case 0 -> Blocks.OAK_PLANKS.defaultBlockState();
            case 1 -> Blocks.QUARTZ_BLOCK.defaultBlockState();
            case 2 -> Blocks.SMOOTH_STONE.defaultBlockState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        };
    }

    /** 大楼层数（按巷间单元哈希，3~7 层） */
    public static int buildingFloors(long seed, int wx, int wz) {
        return CITY_MIN_FLOORS + (int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30303L)
                % (CITY_MAX_FLOORS - CITY_MIN_FLOORS + 1));
    }

    // ---------- 小路 ----------

    private static void placeLanePart(WorldGenLevel level, int wx, int wz, long seed) {
        int rx = relToLane(wx);
        int rz = relToLane(wz);
        boolean xLane = Math.abs(rz) <= LANE_HALF; // 沿 x 方向的路
        boolean zLane = Math.abs(rx) <= LANE_HALF; // 沿 z 方向的路
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        if (xLane && zLane) {
            // 十字路口：边缘是台阶
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

    // ---------- 城市大楼 ----------

    private static void placeCityPart(WorldGenLevel level, int wx, int wz, long seed) {
        int floors = buildingFloors(seed, wx, wz);
        int h = floors * FLOOR_HEIGHT; // 每层 5 格高
        int rx = relToLane(wx);
        int rz = relToLane(wz);
        // 楼区边缘 = 距最近巷中心 |r| == LANE_HALF+1（relToLane 返回对称范围，两侧都成立）
        boolean edgeX = Math.abs(rx) == LANE_HALF + 1;
        boolean edgeZ = Math.abs(rz) == LANE_HALF + 1;
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        if (edgeX || edgeZ) {
            // 外墙：门洞（y1）留空，其余按层交替玻璃/统一墙色（整栋楼一个颜色）
            for (int y = 1; y <= h; y++) {
                if (y == 1 && isDoorGap(wx, wz)) {
                    continue;
                }
                BlockState wall = (y % 2 == 0) ? Blocks.GLASS.defaultBlockState() : wallMaterial(seed, wx, wz);
                level.setBlock(new BlockPos(wx, y, wz), wall, Block.UPDATE_NONE);
            }
        } else if (hash(seed, wx, wz, 0x30304L) % 200 == 0) {
            // 内部设施（哈希）
            BlockState facility = switch ((int) (hash(seed, wx, wz, 0x30305L) % 3)) {
                case 0 -> Blocks.CRAFTING_TABLE.defaultBlockState();
                case 1 -> Blocks.FURNACE.defaultBlockState();
                default -> Blocks.ENCHANTING_TABLE.defaultBlockState();
            };
            level.setBlock(p, facility, Block.UPDATE_NONE);
        }
        // 天花板（整层，y=h+1）
        level.setBlock(new BlockPos(wx, h + 1, wz), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Block.UPDATE_NONE);
    }

    /** 大楼墙色：按巷间单元哈希在生成前抽好（整栋楼统一颜色） */
    private static BlockState wallMaterial(long seed, int wx, int wz) {
        return switch ((int) (hash(seed, cellIndex(wx), cellIndex(wz), 0x30306L) % 4)) {
            case 0 -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case 1 -> Blocks.YELLOW_CONCRETE.defaultBlockState();
            case 2 -> Blocks.RED_CONCRETE.defaultBlockState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        };
    }

    /** 门洞：外墙 y1 每 12 格留 2 宽缺口 */
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

    // ---------- 郊区 ----------

    private static final ResourceKey<ConfiguredFeature<?, ?>> CF_OAK =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace("oak"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> CF_BIRCH =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace("birch"));

    private static void placeSuburbPart(WorldGenLevel level, int wx, int wz, long seed) {
        BlockPos p = new BlockPos(wx, GROUND_Y, wz);
        // 农田块：每 8×8 一块，约 1/3
        if (hash(seed, Math.floorDiv(wx, 8), Math.floorDiv(wz, 8), 0x30307L) % 3 == 0) {
            boolean carrot = hash(seed, wx, wz, 0x30308L) % 2 == 0;
            level.setBlock(p, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_NONE);
            level.setBlock(new BlockPos(wx, GROUND_Y + 1, wz),
                    (carrot ? Blocks.CARROTS : Blocks.WHEAT).defaultBlockState(), Block.UPDATE_NONE);
            return;
        }
        // 树：约 1/30 格一棵，使用原版 configured feature（橡树/白桦交替）
        if (hash(seed, wx, wz, 0x30309L) % 30 == 0) {
            boolean birch = hash(seed, wx, wz, 0x3030AL) % 2 == 0;
            var registry = level.getLevel().registryAccess().lookup(Registries.CONFIGURED_FEATURE).orElseThrow();
            registry.get(birch ? CF_BIRCH : CF_OAK).orElseThrow().value()
                    .place(level, level.getLevel().getChunkSource().getGenerator(), level.getRandom(), p);
        }
    }

    // ---------- 出生大楼（5 层 × 每层 5 格） ----------

    private static void placeSpawnBuildingPart(WorldGenLevel level, int wx, int wz, long seed) {
        int ix = wx - SPAWN_BUILDING_MIN; // 内部坐标 0..24
        int iz = wz - SPAWN_BUILDING_MIN;
        int top = SPAWN_FLOORS * FLOOR_HEIGHT; // 25
        boolean edgeX = ix == 0 || ix == 24;
        boolean edgeZ = iz == 0 || iz == 24;
        boolean shaft = ix >= 3 && ix <= 4 && iz >= 3 && iz <= 4;       // 楼梯井/电梯井 2×2
        boolean elevatorColumn = ix == 2 && iz == 3;                     // 铁块电梯柱
        boolean elevatorPad = ix == 2 && iz == 4;                        // 石英电梯方块（每层）
        boolean stairwell = ix == 3 && iz >= 5 && iz <= 9;               // 楼梯间（1 宽 5 长）

        if (edgeX || edgeZ) {
            // 外墙：蓝色染色玻璃 + 浅蓝混凝土柱（每 4 格），门洞 y1 留空
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
            // 楼梯井/电梯井：y1..25 全部空气
            for (int y = 1; y <= top; y++) {
                level.setBlock(new BlockPos(wx, y, wz), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (elevatorColumn) {
            // 铁块电梯柱
            for (int y = 1; y <= top; y++) {
                level.setBlock(new BlockPos(wx, y, wz), Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (elevatorPad) {
            // 石英电梯方块：每层地板层各一个（装饰+粒子）
            for (int floor = 0; floor < SPAWN_FLOORS; floor++) {
                int y = 1 + floor * FLOOR_HEIGHT;
                level.setBlock(new BlockPos(wx, y, wz), ModBlocks.QUARTZ_ELEVATOR.get().defaultBlockState(), Block.UPDATE_NONE);
            }
            return;
        }
        if (stairwell) {
            // 石英台阶楼梯：每层一段（5 个台阶从低到高），从楼层地板爬到上一层
            for (int floor = 0; floor < SPAWN_FLOORS - 1; floor++) {
                int base = 1 + floor * FLOOR_HEIGHT;
                // iz=9 最低 → iz=5 最高（每格高 1）
                int step = 9 - iz;
                int y = base + step;
                if (y >= base + 1 && y <= base + FLOOR_HEIGHT) {
                    level.setBlock(new BlockPos(wx, y, wz), Blocks.QUARTZ_STAIRS.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
            return;
        }
        // 楼层地板：每层地板层（y = 5k+1），石英/木板交替；层间留空（开放楼层）
        for (int floor = 0; floor < SPAWN_FLOORS; floor++) {
            int y = 1 + floor * FLOOR_HEIGHT;
            level.setBlock(new BlockPos(wx, y, wz),
                    (floor % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.OAK_PLANKS).defaultBlockState(), Block.UPDATE_NONE);
        }
        // 一楼家具（哈希）
        if (hash(seed, wx, wz, 0x3030CL) % 50 == 0) {
            level.setBlock(new BlockPos(wx, GROUND_Y, wz), Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
        }
    }

    /** 出生大楼一楼门洞 */
    private static boolean isSpawnDoor(int wx, int wz) {
        if (wz == SPAWN_BUILDING_MAX && (wx == 10 || wx == 11)) return true;
        if (wz == SPAWN_BUILDING_MIN && (wx == 20 || wx == 21)) return true;
        if (wx == SPAWN_BUILDING_MIN && (wz == 16 || wz == 17)) return true;
        return wx == SPAWN_BUILDING_MAX && (wz == 8 || wz == 9);
    }

    // ---------- 工具 ----------

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
