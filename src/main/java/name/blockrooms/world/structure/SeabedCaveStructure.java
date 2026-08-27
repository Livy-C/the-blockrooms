package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class SeabedCaveStructure extends TemplateScatterStructure {
    public static final MapCodec<SeabedCaveStructure> CODEC = simpleCodec(SeabedCaveStructure::new);
    private static final long SALT = 0x1BADC0DE00000004L;

    public SeabedCaveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "seabed_cave");
    }

    @Override
    protected int sizeX() {
        return 12;
    }

    @Override
    protected int sizeY() {
        return 13;
    }

    @Override
    protected int sizeZ() {
        return 12;
    }

    @Override
    protected int anchorYOffset() {
        return -8;
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SEABED_CAVE_TYPE.get();
    }
}