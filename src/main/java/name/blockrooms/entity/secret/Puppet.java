package name.blockrooms.entity.secret;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;


public class Puppet extends AbstractPuppet {

    public Puppet(EntityType<? extends Puppet> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractPuppet.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D);
    }
}
