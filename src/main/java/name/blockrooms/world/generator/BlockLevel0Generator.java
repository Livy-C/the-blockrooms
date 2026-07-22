package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class BlockLevel0Generator extends BaseBlockLevelGenerator {
    public static final MapCodec<BlockLevel0Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel0Generator::getBiomeSource)
            ).apply(instance, BlockLevel0Generator::new)
    );

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
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                for (int y = this.getMinY(); y <= this.getGenDepth(); y++) {
                    if (y >= 0 && y <= 4) chunk.setBlockState(new BlockPos(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    else chunk.setBlockState(new BlockPos(x, y, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);
                }
                chunk.setBlockState(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_NONE);
                chunk.setBlockState(new BlockPos(x, 1, z), Blocks.BROWN_CARPET.defaultBlockState(), Block.UPDATE_NONE);

                if ((worldX % 5 + 5) % 5 < 2 && (worldZ % 2 + 2) % 2 == 0) {
                    chunk.setBlockState(new BlockPos(x, 5, z), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), Block.UPDATE_ALL);
                    chunk.setBlockState(new BlockPos(x, 6, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                }
                else {
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
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                if (new Random(worldGenRegion.getSeed() ^ (worldX * 0x9e3779b97f4a7c15L) ^ (worldZ * 0xdefacedddeedbeefL)).nextDouble() <= 0.2) {
                    for (int y = 1; y <= 4; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.CHISELED_SANDSTONE.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {

    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }
}
