package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockLevel2Generator extends BaseBlockLevelGenerator {

    public static final int FLOOR_Y = 0;
    public static final int CEILING_Y = 4;
    public static final int CAP_Y = CEILING_Y + 1;
    public static final int INTERIOR_MIN_Y = 1;
    public static final int INTERIOR_MAX_Y = 3;

    private static final int TPL_SIZE = 16;
    private static final int TPL_HEIGHT = 6;

    private static final double EDGE_OPEN_CHANCE = 0.7;

    private static final Identifier TPL_CORRIDOR_X = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_corridor_x");
    private static final Identifier TPL_CORNER = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_corner");
    private static final Identifier TPL_TJUNCTION = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_tjunction");
    private static final Identifier TPL_CROSSROADS = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_crossroads");
    private static final Identifier TPL_DEAD_END = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_deadend");


    public static final Identifier TPL_HEAT_MACHINE = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_heating_iron_block_machine");
    /** 机器房密度：约每 1024 个区块一个（≈32×32 区块、512 格间隔），保证温度系统有缓冲区 */
    public static final int HEAT_MACHINE_DENOM = 1024;
    /** 特殊房间模板（16×16 满区块；机器房独立判定，不在此列）：棺材房 / 惊喜房 */
    private static final Identifier[] SPECIAL_TEMPLATES = {
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_coffin_room"),
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_supplise_room")
    };

    public static final MapCodec<BlockLevel2Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel2Generator::getBiomeSource)
            ).apply(instance, BlockLevel2Generator::new)
    );

    public static final ResourceKey<LootTable> BLOCKLEVEL2_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel2"));

    public BlockLevel2Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        placeWorldTemplate(level, chunk);
        placeDecorations(level, chunk);
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    private static final int CHEST_DENOM = 25;
    private static final int IRON_DENOM = 40;
    private static final int TERRACOTTA_DENOM = 15;
    private static final int SHULKER_ON_IRON_DENOM = 3;
    private static final int SPECIAL_ROOM_CHEST_DENOM = 24;

    /**
     * 隧道装饰还原（确定性哈希，仅普通隧道区块；特殊房间区块由房间规则负责）：
     * <ul>
     *   <li>只在<b>隧道内部靠原始结构墙</b>的地面格（y=1）生成：装饰方块不会被当作"墙"
     *       （杜绝连锁蔓延挤占通道），并要求隧道方向至少 2 格连续空气（宽 2 通道不生成）；</li>
     *   <li><b>箱子</b>：靠墙单格，约 1/25；</li>
     *   <li><b>铁块</b> / <b>红色陶瓦</b>：靠墙<b>沿隧道走向延伸 3~4 格</b>的一排
     *       （约 1/40 / 1/15），排的末端约一半概率叠到 y=2；</li>
     *   <li><b>潜影盒</b>只生成在铁块排起点上方（约 1/3）。</li>
     * </ul>
     */
    private static void placeDecorations(WorldGenLevel level, ChunkAccess chunk) {
        long seed = level.getSeed();
        ChunkPos cp = chunk.getPos();
        // 特殊房间区块：装饰与随机容器由房间规则负责（见 fillSpecialRoomLoot），这里跳过
        if (isSpecialChunk(seed, cp.x, cp.z)) {
            return;
        }
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos p = new BlockPos(minX + x, INTERIOR_MIN_Y, minZ + z);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(Blocks.SMOOTH_STONE)) {
                    continue;
                }
                // 靠原始结构墙 + 隧道方向至少 2 格连续空气（宽 2 通道、隧道中间格不满足 → 不生成）
                if (!wallAdjacent(level, p) || tunnelDir(level, p) == null) {
                    continue;
                }
                long h = hash3(seed, p.getX(), p.getY(), p.getZ());
                if (h % CHEST_DENOM == 0) {
                    placeLootContainer(level, p, Blocks.CHEST.defaultBlockState(), seed ^ p.asLong());
                } else if (h % IRON_DENOM == 5) {
                    placeDecoRun(level, seed, p, Blocks.IRON_BLOCK);
                } else if (h % TERRACOTTA_DENOM == 10) {
                    placeDecoRun(level, seed, p, Blocks.RED_TERRACOTTA);
                }
            }
        }
    }

    /**
     * 生成一排装饰（铁块/陶瓦）：沿隧道走向延伸 3~4 格（向前后随机分配），
     * 末端约一半概率在 y=2 再叠一格；铁块排起点上方按概率生成潜影盒。
     * 路径上任一格不合法（非空气/无地板/不靠墙/通道过窄）则放弃整排。
     */
    private static void placeDecoRun(WorldGenLevel level, long seed, BlockPos start, Block block) {
        Direction dir = tunnelDir(level, start);
        if (dir == null) {
            return;
        }
        long h = hash3(seed, start.getX(), start.getY(), start.getZ());
        int total = 3 + (int) (h % 2);                     // 排总长 3~4 格
        int forward = 1 + (int) ((h >>> 8) % (total - 1)); // 向前 1..total-1
        int backward = total - 1 - forward;                // 向后补足
        BlockPos tip = null;
        for (int i = -backward; i <= forward; i++) {
            BlockPos q = start.relative(dir, i);
            if (!level.getBlockState(q).isAir()
                    || !level.getBlockState(q.below()).is(Blocks.SMOOTH_STONE)
                    || !wallAdjacent(level, q)
                    || tunnelDir(level, q) == null) {
                return; // 路径上有非法格 → 放弃整排，保持简单
            }
            tip = q;
        }
        for (int i = -backward; i <= forward; i++) {
            level.setBlock(start.relative(dir, i), block.defaultBlockState(), Block.UPDATE_NONE);
        }
        // 末端约一半概率叠到 y=2（铁块/陶瓦通用）
        if ((h & 1) == 0 && tip != null && level.getBlockState(tip.above()).isAir()) {
            level.setBlock(tip.above(), block.defaultBlockState(), Block.UPDATE_NONE);
        }
        // 潜影盒：只生成在铁块上方
        if (block == Blocks.IRON_BLOCK
                && hash2(seed, start.getX(), start.getZ()) % SHULKER_ON_IRON_DENOM == 0
                && level.getBlockState(start.above()).isAir()) {
            placeLootContainer(level, start.above(), Blocks.SHULKER_BOX.defaultBlockState(), seed ^ start.above().asLong());
        }
    }

    /**
     * 隧道走向：水平 4 方向中"前方 2 格都是空气"的方向（即隧道内部方向）。
     * 多个方向（十字口）时按位置哈希固定选一个；无方向（宽 2 通道/死路尽端）返回 null。
     */
    private static Direction tunnelDir(WorldGenLevel level, BlockPos p) {
        List<Direction> dirs = new ArrayList<>(4);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(p.relative(d)).isAir()
                    && level.getBlockState(p.relative(d, 2)).isAir()) {
                dirs.add(d);
            }
        }
        if (dirs.isEmpty()) {
            return null;
        }
        return dirs.get((int) Math.floorMod(p.asLong(), dirs.size()));
    }

    /** 该区块是否为特殊房间区块（与生成逻辑同一哈希判定） */
    private static boolean isSpecialChunk(long seed, int chunkX, int chunkZ) {
        return hashChunk(seed, chunkX, chunkZ) % 12 == 0;
    }

    /** 靠墙：水平 4 方向至少一侧是<b>原始结构方块</b>（装饰方块不算墙，杜绝连锁蔓延） */
    private static boolean wallAdjacent(WorldGenLevel level, BlockPos p) {
        return isStructureWall(level.getBlockState(p.north()))
                || isStructureWall(level.getBlockState(p.south()))
                || isStructureWall(level.getBlockState(p.east()))
                || isStructureWall(level.getBlockState(p.west()));
    }

    private static boolean isStructureWall(BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.IRON_BLOCK)
                && !state.is(Blocks.RED_TERRACOTTA)
                && !state.is(Blocks.CHEST)
                && !state.is(Blocks.SHULKER_BOX);
    }

    private static long hash2(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xBF58476D1CE4E5B9L) ^ 0x2B5E17L;
        h = (h ^ (h >>> 33)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 29)) & Long.MAX_VALUE;
    }

    private static long hash3(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xBF58476D1CE4E5B9L) ^ (z * 0x165667B19E3779F9L) ^ 0x3C6EF372L;
        h = (h ^ (h >>> 33)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 29)) & Long.MAX_VALUE;
    }

    private void placeWorldTemplate(WorldGenLevel level, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();

        boolean north = edgeOpen(seed, cp.x, cp.z, Direction.NORTH);
        boolean east = edgeOpen(seed, cp.x, cp.z, Direction.EAST);
        boolean south = edgeOpen(seed, cp.x, cp.z, Direction.SOUTH);
        boolean west = edgeOpen(seed, cp.x, cp.z, Direction.WEST);

        int count = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);

        Identifier tpl;
        Rotation rotation = Rotation.NONE;
        // 机器房：独立低概率判定（约 1/1024 区块），优先于普通特殊房间
        boolean heatMachine = isHeatMachineChunk(seed, cp.x, cp.z);
        boolean special = !heatMachine && hashChunk(seed, cp.x, cp.z) % 12 == 0;
        if (heatMachine) {
            tpl = TPL_HEAT_MACHINE;
        } else if (special) {
            tpl = SPECIAL_TEMPLATES[(int) ((hashChunk(seed, cp.x, cp.z) >>> 8) % SPECIAL_TEMPLATES.length)];
        } else {
            switch (count) {
            case 4 -> tpl = TPL_CROSSROADS;
            case 3 -> {
                tpl = TPL_TJUNCTION;
                rotation = Rotation.values()[missingSide(north, east, south, west)];
            }
            case 2 -> {
                if (north && south) {
                    tpl = TPL_CORRIDOR_X;
                    rotation = Rotation.CLOCKWISE_90;
                } else if (east && west) {
                    tpl = TPL_CORRIDOR_X;
                } else {
                    // 拐角：模板基准为东南口（东=1、南=2），a 为两口中顺时针序靠前的一个
                    tpl = TPL_CORNER;
                    int a = north ? (east ? 0 : 3) : (east ? 1 : 2);
                    rotation = Rotation.values()[Math.floorMod(a - 1, 4)];
                }
            }
            case 1 -> {
                tpl = TPL_DEAD_END;
                rotation = Rotation.values()[Math.floorMod(singleSide(north, east, south, west) - 3, 4)];
            }
            default -> {
                tpl = TPL_DEAD_END;
                int d = Math.floorMod(hashChunk(seed, cp.x, cp.z), 4);
                rotation = Rotation.values()[Math.floorMod(d - 3, 4)];
            }
            }
        }

        StructureTemplate template;
        try {
            template = level.getLevel().getServer().getStructureManager().get(tpl).orElse(null);
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL2-TPL: chunk {} failed to load template {}: {}", cp, tpl, e.toString());            template = null;
        }
        BlockPos origin = new BlockPos(cp.getMinBlockX(), 0, cp.getMinBlockZ());
        // 新版特殊房间均为 16×16 满区块尺寸，与普通模板同原点放置（旧 12×12 居中偏移已移除）
        if (template == null) {
            // 模板缺失（尚未搭建/文件名不符）：实心石砖保底，避免玩家掉进虚空
            Blockrooms.LOGGER.warn("BL2-TPL: chunk {} template {} not found -> solid fallback", cp, tpl);
            fillFallback(chunk);
            return;
        }
        try {
            int placed = placeTemplateRotated(level, template, origin, rotation);
            Blockrooms.LOGGER.debug("BL2-TPL: chunk {} template {} rot={} placed={}", cp, tpl, rotation, placed);
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL2-TPL: chunk {} template {} placement failed", cp, tpl, e);
            fillFallback(chunk);
        }
        applyLoot(level, origin, special);
    }

    private static int placeTemplateRotated(WorldGenLevel level, StructureTemplate template, BlockPos origin, Rotation rotation) {
        BlockPos offset = switch (rotation) {
            case CLOCKWISE_90 -> origin.offset(15, 0, 0);
            case CLOCKWISE_180 -> origin.offset(15, 0, 15);
            case COUNTERCLOCKWISE_90 -> origin.offset(0, 0, 15);
            default -> origin;
        };
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        boolean placed = template.placeInWorld(level, offset, origin, settings, level.getRandom(), Block.UPDATE_NONE);
        return placed ? 1 : 0;
    }

    private static boolean edgeOpen(long seed, int chunkX, int chunkZ, Direction dir) {
        int ex = chunkX;
        int ez = chunkZ;
        if (dir == Direction.WEST) {
            ex = chunkX - 1;
            dir = Direction.EAST;
        } else if (dir == Direction.NORTH) {
            ez = chunkZ - 1;
            dir = Direction.SOUTH;
        }
        long h = seed ^ (ex * 0x9E3779B97F4A7C15L) ^ (ez * 0xC2B2AE3D27D4EB4FL) ^ (dir.get2DDataValue() * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 32)) & 0xFFFF) / 65536.0 < EDGE_OPEN_CHANCE;
    }

    private static int missingSide(boolean north, boolean east, boolean south, boolean west) {
        if (!north) return 0;
        if (!east) return 1;
        if (!south) return 2;
        return 3;
    }

    private static int singleSide(boolean north, boolean east, boolean south, boolean west) {
        if (north) return 0;
        if (east) return 1;
        if (south) return 2;
        return 3;
    }

    private static long hashChunk(long seed, int chunkX, int chunkZ) {
        long h = seed ^ (chunkX * 0x9E3779B97F4A7C15L) ^ (chunkZ * 0xBF58476D1CE4E5B9L);
        h = (h ^ (h >>> 33)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    /**
     * 该区块是否为红热铁块机器房（独立判定，与 {@link #placeWorldTemplate} 同一哈希）：
     * 约每 {@link #HEAT_MACHINE_DENOM} 个区块一个（≈32×32 区块间隔），
     * 保证机器热场之间有足够的缓冲区。
     */
    public static boolean isHeatMachineChunk(long seed, int chunkX, int chunkZ) {
        long h = hashChunk(seed, chunkX, chunkZ);
        return (h & Long.MAX_VALUE) % HEAT_MACHINE_DENOM == 0;
    }

    public static BlockPos heatMachineCenter(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
    }

    private static void applyLoot(WorldGenLevel level, BlockPos origin, boolean special) {
        long seed = level.getSeed();
        for (int x = 0; x < TPL_SIZE; x++) {
            for (int z = 0; z < TPL_SIZE; z++) {
                BlockPos p = origin.offset(x, INTERIOR_MIN_Y, z);
                if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity container) {
                    container.setLootTable(BLOCKLEVEL2_LOOT);
                    container.setLootTableSeed(seed ^ p.asLong());
                }
            }
        }
        if (special) {
            fillSpecialRoomLoot(level, origin);
        }
    }

    private static void fillSpecialRoomLoot(WorldGenLevel level, BlockPos origin) {
        long seed = level.getSeed();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                BlockPos p = origin.offset(x, INTERIOR_MIN_Y, z);
                if (!level.getBlockState(p).isAir()) {
                    continue;
                }
                if (!level.getBlockState(p.below()).is(Blocks.SMOOTH_STONE)
                        && !level.getBlockState(p.below()).is(Blocks.STONE_BRICKS)) {
                    continue;
                }
                long h = hash3(seed, p.getX(), p.getY(), p.getZ());
                if (h % SPECIAL_ROOM_CHEST_DENOM == 0) {
                    placeLootContainer(level, p, Blocks.CHEST.defaultBlockState(), seed ^ p.asLong());
                }
            }
        }
    }

    private static void placeLootContainer(WorldGenLevel level, BlockPos pos, BlockState state, long lootSeed) {
        level.setBlock(pos, state, Block.UPDATE_NONE);
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            if (entityBlock.newBlockEntity(pos, state) instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(BLOCKLEVEL2_LOOT);
                container.setLootTableSeed(lootSeed);
                level.getChunk(pos).setBlockEntity(container);
            }
        }
    }

    private static void fillFallback(ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= CAP_Y; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z),
                            y == CAP_Y ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(),
                            Block.UPDATE_NONE);
                }
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 结构模板自包含（含箱子/装饰）；此阶段无需额外生成
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}
