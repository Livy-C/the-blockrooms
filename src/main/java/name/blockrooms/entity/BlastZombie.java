package name.blockrooms.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 爆破僵尸：BlockLevel 13.8 的僵尸变种。
 * <ul>
 *   <li>主手 TNT、副手打火石、身穿鞘翅；</li>
 *   <li><b>可以飞行</b>：无重力 + 飞行寻路 + 飞行移动控制（完整飞行 AI）；</li>
 *   <li>免疫所有爆炸伤害；</li>
 *   <li>追踪玩家并<b>在玩家周围引爆</b>（AoE 爆炸伤害，不破坏地形）；</li>
 *   <li>玩家聊天时定位并锁定玩家（见 BlockLevel13Point8Handler）。</li>
 * </ul>
 */
public class BlastZombie extends Zombie {
    private static final double ATTACK_RANGE = 6.0;
    private static final int ATTACK_COOLDOWN = 80;

    public BlastZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        // 装备
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TNT));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FLINT_AND_STEEL));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        // 替换 AI：飞行追逐 + 接近引爆
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.moveControl = new BlastFlightMoveControl(this);
        this.goalSelector.addGoal(1, new BlastAttackGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return false; // 免疫所有爆炸伤害
        }
        return super.hurtServer(level, source, amount);
    }

    /** 飞行移动控制：直线加速飞向寻路目标点 */
    private static class BlastFlightMoveControl extends MoveControl {
        private final BlastZombie mob;

        BlastFlightMoveControl(BlastZombie mob) {
            super(mob);
            this.mob = mob;
        }

        @Override
        public void tick() {
            if (this.operation != Operation.MOVE_TO) {
                return;
            }
            Vec3 delta = new Vec3(this.getWantedX() - mob.getX(), this.getWantedY() - mob.getY(), this.getWantedZ() - mob.getZ());
            double len = delta.length();
            if (len < 0.6) {
                mob.setSpeed(0.0F);
                return;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.85).add(delta.scale(0.05)));
            mob.setYRot(-(float) (Mth.atan2(delta.x, delta.z) * (180.0 / Math.PI)));
            mob.yBodyRot = mob.getYRot();
        }
    }

    /** 攻击：飞向玩家，接近 3 格内引爆 AoE（不破坏地形） */
    private class BlastAttackGoal extends Goal {
        private final BlastZombie mob;
        private int cooldown;

        BlastAttackGoal(BlastZombie mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target instanceof Player && target.isAlive() && --this.cooldown <= 0;
        }

        @Override
        public void start() {
            this.cooldown = 40;
        }

        @Override
        public void stop() {
            this.mob.setTarget(null);
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            if (this.mob.distanceToSqr(target) < 9.0) {
                explodeNearby();
                this.cooldown = ATTACK_COOLDOWN;
            } else {
                this.mob.getNavigation().moveTo(target, 1.0);
            }
        }

        private void explodeNearby() {
            ServerLevel level = (ServerLevel) this.mob.level();
            level.sendParticles(ParticleTypes.EXPLOSION,
                    this.mob.getX(), this.mob.getY(), this.mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            for (Player p : level.players()) {
                if (p.distanceToSqr(this.mob) < ATTACK_RANGE * ATTACK_RANGE) {
                    p.hurtServer(level, level.damageSources().explosion(this.mob, this.mob), 6.0F);
                }
            }
        }
    }
}
