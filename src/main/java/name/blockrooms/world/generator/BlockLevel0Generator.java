package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * BL0（Level 0）生成器——暂保留程序化骨架，等待结构模板化重写。
 *
 * <p>当前内容（待模板化后替换）：
 * 橡木地板（y=0）+ 棕色地毯（y=1）+ 红石灯/红石块天花板（y=5/6），
 * 以及区块级变体（灯灭/地毯缺块/天花板洞）。画与前哨站已移除（前哨站之后以模板重新加入）。</p>
 */
public class BlockLevel0Generator extends BaseBlockLevelGenerator {
    public static final MapCodec<BlockLevel0Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel0Generator::getBiomeSource)
            ).apply(instance, BlockLevel0Generator::new)
    );

    private static final double LAMP_OFF_CHANCE = 0.10;
    private static final double CARPET_GAP_CHANCE = 0.10;
    private static final double CEILING_HOLE_CHANCE = 0.05;

    public BlockLevel0Generator(BiomeSource biomeSource) {
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
                for (int y = this.getMinY(); y <= this.getGenDepth(); y++) {
                    if (y >= 0 && y <= 4) chunk.setBlockState(new BlockPos(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    else chunk.setBlockState(new BlockPos(x, y, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);
                }
                chunk.setBlockState(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_NONE);
                chunk.setBlockState(new BlockPos(x, 1, z), Blocks.BROWN_CARPET.defaultBlockState(), Block.UPDATE_NONE);

                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;
                if ((worldX % 5 + 5) % 5 < 2 && (worldZ % 2 + 2) % 2 == 0) {
                    chunk.setBlockState(new BlockPos(x, 5, z), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), Block.UPDATE_ALL);
                    chunk.setBlockState(new BlockPos(x, 6, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                } else {
                    chunk.setBlockState(new BlockPos(x, 5, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        applyVariantRegions(worldGenRegion.getSeed(), chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }

    /**
     * 区块级变体（模板化后仍由生成器处理）：灯灭、地毯缺块、天花板洞。
     */
    private static void applyVariantRegions(long seed, ChunkAccess chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        Random random = new Random(seed ^ (chunkX * 0x9e3779b97f4a7c15L) ^ (chunkZ * 0xdefacedddeedbeefL));

        boolean lampsOff = random.nextDouble() < LAMP_OFF_CHANCE;
        boolean carpetGaps = random.nextDouble() < CARPET_GAP_CHANCE;
        boolean ceilingHole = random.nextDouble() < CEILING_HOLE_CHANCE;
        if (!lampsOff && !carpetGaps && !ceilingHole) return;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (lampsOff) {
                    BlockPos lampPos = new BlockPos(x, 5, z);
                    BlockState lamp = chunk.getBlockState(lampPos);
                    if (lamp.is(Blocks.REDSTONE_LAMP)) {
                        chunk.setBlockState(lampPos, lamp.setValue(RedstoneLampBlock.LIT, false), Block.UPDATE_CLIENTS);
                        chunk.setBlockState(new BlockPos(x, 6, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                if (carpetGaps && random.nextDouble() < 0.25) {
                    BlockPos carpetPos = new BlockPos(x, 1, z);
                    if (chunk.getBlockState(carpetPos).is(Blocks.BROWN_CARPET)) {
                        chunk.setBlockState(carpetPos, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        if (ceilingHole) {
            int holeX = random.nextInt(14);
            int holeZ = random.nextInt(14);
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    int bx = holeX + dx;
                    int bz = holeZ + dz;
                    if (chunk.getBlockState(new BlockPos(bx, 5, bz)).is(Blocks.REDSTONE_LAMP)) {
                        chunk.setBlockState(new BlockPos(bx, 6, bz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    chunk.setBlockState(new BlockPos(bx, 5, bz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
