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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;

public class BlockLevel542Generator extends BaseBlockLevelGenerator {
    public static final MapCodec<BlockLevel542Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel542Generator::getBiomeSource)
            ).apply(instance, BlockLevel542Generator::new)
    );

    public static final int CITY_HALF = 768;
    public static final int STREET_GRID = 48;
    public static final int BUILDING_HALF = 22;

    public BlockLevel542Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        ChunkAccess c = chunk;
        int minX = c.getPos().getMinBlockX();
        int minZ = c.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = minX + x;
                int wz = minZ + z;
                boolean city = Math.abs(wx) <= CITY_HALF && Math.abs(wz) <= CITY_HALF;
                BlockState ground;
                if (city) {
                    // 街道：网格线上用浅灰色混凝土，其余灰色混凝土（楼房地基在网格内，后续模板堆叠）
                    boolean street = Math.floorMod(wx, STREET_GRID) == 0 || Math.floorMod(wz, STREET_GRID) == 0;
                    ground = street ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()
                            : Blocks.GRAY_CONCRETE.defaultBlockState();
                } else {
                    ground = Blocks.GRASS_BLOCK.defaultBlockState();
                }
                c.setBlockState(new BlockPos(x, 0, z), ground, Block.UPDATE_NONE);
                // 基岩底：y=0 下方没有（min_y=0）；地下层后续模块再挖
            }
        }
        return CompletableFuture.completedFuture(chunk);
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
