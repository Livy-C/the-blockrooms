package name.blockrooms.world.generator;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.Function;

public abstract class BlockLevelGenerator extends ChunkGenerator {
    public BlockLevelGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public BlockLevelGenerator(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter) {
        super(biomeSource, generationSettingsGetter);
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return -64;
    }
}
