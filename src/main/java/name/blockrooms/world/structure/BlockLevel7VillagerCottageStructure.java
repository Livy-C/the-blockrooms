package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class BlockLevel7VillagerCottageStructure extends TemplateScatterStructure {
    public static final MapCodec<BlockLevel7VillagerCottageStructure> CODEC = simpleCodec(BlockLevel7VillagerCottageStructure::new);
    private static final long SALT = 0x1BADC0DE00000002L;

    public BlockLevel7VillagerCottageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "blocklevel7_villager_cottage");
    }

    @Override
    protected int sizeX() {
        return 9;
    }

    @Override
    protected int sizeY() {
        return 8;
    }

    @Override
    protected int sizeZ() {
        return 9;
    }

    @Override
    protected int anchorYOffset() {
        return 0;
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BL7_COTTAGE_TYPE.get();
    }
}