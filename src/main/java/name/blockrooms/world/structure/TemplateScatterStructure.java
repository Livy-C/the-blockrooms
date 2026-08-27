package name.blockrooms.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public abstract class TemplateScatterStructure extends Structure {

    private static final int MIN_OFFSET = 2;
    private static final int MAX_OFFSET = 13;

    protected TemplateScatterStructure(StructureSettings settings) {
        super(settings);
    }

    protected abstract Identifier templateId();

    protected abstract int sizeX();

    protected abstract int sizeY();

    protected abstract int sizeZ();

    protected abstract int anchorYOffset();

    protected abstract long salt();

    protected long spacing() {
        return 1;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long hash = scatterHash(context.seed(), chunkPos.x, chunkPos.z, salt());
        if (Math.floorMod(hash, spacing()) != 0) {
            return Optional.empty();
        }
        int dx = MIN_OFFSET + (int) ((hash >>> 8) % (MAX_OFFSET - MIN_OFFSET + 1));
        int dz = MIN_OFFSET + (int) ((hash >>> 16) % (MAX_OFFSET - MIN_OFFSET + 1));
        Rotation rotation = Rotation.values()[(int) ((hash >>> 24) % 4)];

        int x = chunkPos.getMinBlockX() + dx;
        int z = chunkPos.getMinBlockZ() + dz;
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState()) + anchorYOffset();
        BlockPos anchor = new BlockPos(x, y, z);

        return Optional.of(new GenerationStub(anchor, builder ->
                builder.addPiece(new NbtTemplatePiece(templateId(), anchor, rotation, sizeX(), sizeY(), sizeZ()))));
    }

    private static long scatterHash(long seed, int chunkX, int chunkZ, long salt) {
        long h = seed ^ salt ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L);
        h = (h ^ (h >>> 33)) * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }
}