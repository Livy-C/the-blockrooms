package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
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
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.concurrent.CompletableFuture;


public class BlockLevel15Generator extends BaseBlockLevelGenerator {

    public static final int BEDROCK_Y = 0;
    public static final int GROUND_Y = 1;
    /** 网格尺寸：每 64×64 格一个候选拱门，锚点在网格内随机偏移 */
    public static final int ARCH_GRID = 64;
    /** 拱门高度范围（格）：5 ~ 100 */
    public static final int ARCH_MIN_H = 5;
    public static final int ARCH_MAX_H = 100;
    /** 拱门最小跨度（格） */
    public static final int ARCH_MIN_SPAN = 3;
    /** 锚点相对网格中心的最大随机偏移（格） */
    public static final int ANCHOR_OFFSET = 24;

    public static final ResourceKey<LootTable> BL15_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel15"));

    public static final MapCodec<BlockLevel15Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel15Generator::getBiomeSource)
            ).apply(instance, BlockLevel15Generator::new)
    );

    public BlockLevel15Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlockState(new BlockPos(x, BEDROCK_Y, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);
                chunk.setBlockState(new BlockPos(x, GROUND_Y, z), Blocks.TUFF.defaultBlockState(), Block.UPDATE_NONE);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();

        placeArchIfPresent(level, chunk, cp, seed);

        placePortalPoint(level, chunk, cp, seed);

        if (hash(seed, cp.x, cp.z, 0x15) % 8 == 0) {
            placeWaterCross(level, chunk, cp, seed);
        }

        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    // ---------- 拱门 ----------
    private record ArchInfo(long seed, int gx, int gz, int ax, int az, int dx, int dz, int span, int h) {
    }

    private void placeArchIfPresent(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        // 拱门最长约 126 格（锚点偏移 24 + 跨度 100 + 柱 2），检查周围 ±2 网格足够覆盖
        int gx0 = Math.floorDiv(cp.getMinBlockX(), ARCH_GRID);
        int gz0 = Math.floorDiv(cp.getMinBlockZ(), ARCH_GRID);
        for (int gx = gx0 - 2; gx <= gx0 + 2; gx++) {
            for (int gz = gz0 - 2; gz <= gz0 + 2; gz++) {
                if (!archExists(seed, gx, gz)) {
                    continue;
                }
                placeArchPart(level, chunk, archInfo(seed, gx, gz));
            }
        }
    }

    /**
     * 把拱门落在本区块内的部分写入（纯几何判定：每个区块只生成自己范围内的方块，
     * 因此跨区块的长拱门也能完整生成）。
     */
    private static void placeArchPart(WorldGenLevel level, ChunkAccess chunk, ArchInfo a) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int type = columnType(a, x, z);
                if (type == 0) {
                    continue;
                }
                // 柱：GROUND_Y .. h+2（实心到顶）；梁：h+1 .. h+2（厚 2）
                int yStart = (type == 1) ? GROUND_Y : GROUND_Y + a.h + 1;
                for (int y = yStart; y <= GROUND_Y + a.h + 2; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.TUFF.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }
        // 补给箱：横梁中点上方，约 1/3 概率
        if (hash(a.seed(), a.gx(), a.gz(), 0x35) % 3 == 0) {
            int midX = a.ax() + a.dx() * (a.span() / 2);
            int midZ = a.az() + a.dz() * (a.span() / 2);
            BlockPos chestPos = new BlockPos(midX, GROUND_Y + a.h() + 3, midZ);
            ChunkPos cp = chunk.getPos();
            if (chestPos.getX() >> 4 == cp.x && chestPos.getZ() >> 4 == cp.z) {
                level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_NONE);
                if (level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity container) {
                    container.setLootTable(BL15_LOOT);
                    container.setLootTableSeed(a.seed() ^ chestPos.asLong());
                }
            }
        }
    }

    /**
     * (x, z) 列在拱门中的结构类型：0 = 无，1 = 柱，2 = 梁。
     * 结构在水平面上是沿方向延伸的 2 宽矩形条：t 为沿方向投影、w 为垂直投影（0/1 两列）。
     */
    private static int columnType(ArchInfo a, int x, int z) {
        int tx = x - a.ax();
        int tz = z - a.az();
        int t = tx * a.dx() + tz * a.dz();
        int w = tx * -a.dz() + tz * a.dx();
        if (w < 0 || w > 1) {
            return 0;
        }
        if (t >= -1 && t <= 0) {
            return 1;
        }
        if (t >= a.span() && t <= a.span() + 1) {
            return 1;
        }
        if (t >= 1 && t <= a.span() - 1) {
            return 2;
        }
        return 0;
    }

    /** 拱门是否存在于该网格（确定性，70%） */
    public static boolean archExists(long seed, int gx, int gz) {
        return hash(seed, gx, gz, 0x15) % 10 < 7;
    }

    /** 拱门高度（确定性，5~100） */
    public static int archHeight(long seed, int gx, int gz) {
        return ARCH_MIN_H + (int) (hash(seed, gx, gz, 0x25) % (ARCH_MAX_H - ARCH_MIN_H + 1));
    }

    /** 拱门跨度（确定性，3~高度，长度不超过高度） */
    public static int archSpan(long seed, int gx, int gz) {
        int h = archHeight(seed, gx, gz);
        return ARCH_MIN_SPAN + (int) (hash(seed, gx, gz, 0x75) % (h - ARCH_MIN_SPAN + 1));
    }

    /** 拱门方向（确定性，0=北 1=东 2=南 3=西） */
    public static int archDirection(long seed, int gx, int gz) {
        return (int) (hash(seed, gx, gz, 0x85) % 4);
    }

    /** 拱门锚点（网格中心 + 随机偏移） */
    public static BlockPos archAnchor(long seed, int gx, int gz) {
        int ax = gx * ARCH_GRID + ARCH_GRID / 2 + (int) (hash(seed, gx, gz, 0x95) % (ANCHOR_OFFSET * 2 + 1)) - ANCHOR_OFFSET;
        int az = gz * ARCH_GRID + ARCH_GRID / 2 + (int) (hash(seed, gx, gz, 0xA5) % (ANCHOR_OFFSET * 2 + 1)) - ANCHOR_OFFSET;
        return new BlockPos(ax, 0, az);
    }

    /** 横梁中点上方（补给箱/传送落点） */
    public static BlockPos archMidTop(long seed, int gx, int gz) {
        ArchInfo a = archInfo(seed, gx, gz);
        return new BlockPos(
                a.ax() + a.dx() * (a.span() / 2),
                GROUND_Y + a.h() + 3,
                a.az() + a.dz() * (a.span() / 2));
    }

    private static ArchInfo archInfo(long seed, int gx, int gz) {
        int dir = archDirection(seed, gx, gz);
        int dx = switch (dir) {
            case 1 -> 1;
            case 2 -> 0;
            case 3 -> -1;
            default -> 0;
        };
        int dz = switch (dir) {
            case 0 -> -1;
            case 1 -> 0;
            case 2 -> 1;
            default -> 0;
        };
        BlockPos anchor = archAnchor(seed, gx, gz);
        return new ArchInfo(seed, gx, gz, anchor.getX(), anchor.getZ(), dx, dz,
                archSpan(seed, gx, gz), archHeight(seed, gx, gz));
    }

    // ---------- 传送点 ----------

    private void placePortalPoint(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        int px = 2 + (int) (hash(seed, cp.x, cp.z, 0x45) % 12);
        int pz = 2 + (int) (hash(seed, cp.x, cp.z, 0x55) % 12);
        BlockPos pos = new BlockPos(cp.getMinBlockX() + px, GROUND_Y, cp.getMinBlockZ() + pz);

        level.setBlock(pos, Blocks.END_PORTAL.defaultBlockState(), Block.UPDATE_NONE);
    }

    // ---------- 十字水坑 ----------

    private void placeWaterCross(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        int wx = 3 + (int) (hash(seed, cp.x, cp.z, 0x65) % 10);
        int wz = 3 + (int) (hash(seed, cp.x, cp.z, 0x75) % 10);
        BlockPos c = new BlockPos(cp.getMinBlockX() + wx, GROUND_Y, cp.getMinBlockZ() + wz);
        BlockState water = Blocks.WATER.defaultBlockState();
        level.setBlock(c, water, Block.UPDATE_NONE);
        level.setBlock(c.east(), water, Block.UPDATE_NONE);
        level.setBlock(c.west(), water, Block.UPDATE_NONE);
        level.setBlock(c.north(), water, Block.UPDATE_NONE);
        level.setBlock(c.south(), water, Block.UPDATE_NONE);
        level.setBlock(c.east().east(), water, Block.UPDATE_NONE);
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
