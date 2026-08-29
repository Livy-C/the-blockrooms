package name.blockrooms.entity.secret;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 通用傀儡基类：被"块室意志"同化的生物。
 * - 血量 = 被转化时原生物最大生命值的 2 倍（由 spawn 时设置）
 * - 攻击默认 20
 * - 被 Boss 操控，主动攻击玩家
 * 子类只需提供渲染器（变体由实体类型区分）。
 */
public abstract class AbstractPuppet extends Monster {

    public static final double DEFAULT_ATTACK = 20.0D;

    public AbstractPuppet(EntityType<? extends AbstractPuppet> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, DEFAULT_ATTACK)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    /** 转化：把源生物的生命值翻倍作为傀儡血量 */
    public void convertFrom(LivingEntity source) {
        double doubled = source.getMaxHealth() * 2.0;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(doubled);
        this.setHealth((float) doubled);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        // 寻找目标：优先攻击最近玩家
        if (this.getTarget() == null || !this.getTarget().isAlive()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                var player = serverLevel.getNearestPlayer(this, 48.0);
                if (player != null) this.setTarget(player);
            }
        }
        LivingEntity target = this.getTarget();
        if (target != null && this.distanceToSqr(target) < 2.5 * 2.5 && this.tickCount % 20 == 0) {
            target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }
}
