package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class RaftStructure extends TemplateScatterStructure {
    public static final MapCodec<RaftStructure> CODEC = simpleCodec(RaftStructure::new);
    private static final long SALT = 0x1BADC0DE00000003L;

    public RaftStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "raft");
    }

    @Override
    protected int sizeX() {
        return 9;
    }

    @Override
    protected int sizeY() {
        return 6;
    }

    @Override
    protected int sizeZ() {
        return 6;
    }

    @Override
    protected int anchorYOffset() {
        return 1;
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RAFT_TYPE.get();
    }
}