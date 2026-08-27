package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class SpruceDoorStructure extends Structure {
    public static final MapCodec<SpruceDoorStructure> CODEC = simpleCodec(SpruceDoorStructure::new);

    private static final long DOOR_SPACING = 8;

    public SpruceDoorStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long hash = doorHash(context.seed(), chunkPos.x, chunkPos.z, 0x9E3779B97F4A7C15L);
        if (Math.floorMod(hash, DOOR_SPACING) != 0) {
            return Optional.empty();
        }
        int dx = 2 + (int) Math.floorMod(hash >>> 8, 12);
        int dz = 2 + (int) Math.floorMod(hash >>> 16, 12);
        Direction facing = Direction.from2DDataValue((int) Math.floorMod(hash >>> 24, 4));
        BlockPos anchor = new BlockPos(chunkPos.getMinBlockX() + dx, 1, chunkPos.getMinBlockZ() + dz);
        return Optional.of(new GenerationStub(anchor,
                builder -> builder.addPiece(new BlockLevel2DoorPiece(anchor, facing, false, ModLevels.BLOCKLEVEL_NULL))));
    }

    protected static long doorHash(long seed, int chunkX, int chunkZ, long salt) {
        long h = seed ^ salt ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 29);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SPRUCE_DOOR_TYPE.get();
    }
}