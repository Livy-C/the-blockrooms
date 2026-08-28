package name.blockrooms.entity.secret;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;

public class SecretEntityOne extends Vex {

    private static final double SCALE = 200.0;
    private static final int CHAIN_DIST = 3;
    private static final int CHAIN_LENGTH = 4;

    private boolean chainsPlaced;

    public SecretEntityOne(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        var scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(SCALE);
        }
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes().add(Attributes.MAX_HEALTH, (double)1024.0F).add(Attributes.ATTACK_DAMAGE, (double)100.0F);
    }

}