package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class QuartzDoorStructure extends Structure {
    public static final MapCodec<QuartzDoorStructure> CODEC = simpleCodec(QuartzDoorStructure::new);

    private static final long DOOR_SPACING = 8;

    public QuartzDoorStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long hash = SpruceDoorStructure.doorHash(context.seed(), chunkPos.x, chunkPos.z, 0x94D049BB133111EBL);
        if (Math.floorMod(hash, DOOR_SPACING) != 0) {
            return Optional.empty();
        }
        int dx = 2 + (int) Math.floorMod(hash >>> 8, 12);
        int dz = 2 + (int) Math.floorMod(hash >>> 16, 12);
        Direction facing = Direction.from2DDataValue((int) Math.floorMod(hash >>> 24, 4));
        BlockPos anchor = new BlockPos(chunkPos.getMinBlockX() + dx, 1, chunkPos.getMinBlockZ() + dz);
        return Optional.of(new GenerationStub(anchor,
                builder -> builder.addPiece(new BlockLevel2DoorPiece(anchor, facing, true, ModLevels.BLOCKLEVEL_1))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.QUARTZ_DOOR_TYPE.get();
    }
}