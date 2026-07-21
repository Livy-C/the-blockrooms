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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockLevel1Generator extends BlockLevelGenerator{
    public static final MapCodec<BlockLevel1Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel1Generator::getBiomeSource)
            ).apply(instance, BlockLevel1Generator::new)
    );

    public BlockLevel1Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }
    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
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
    public int getGenDepth() {
        return 384;
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
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        return null;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {

    }
}
