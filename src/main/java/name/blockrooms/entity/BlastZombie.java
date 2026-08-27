package name.blockrooms.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BlastZombie extends Zombie {
    private static final double EXPLODE_RANGE_SQ = 16.0;
    private static final double WALK_RANGE_SQ = 100.0;
    private static final double ATTACK_COOLDOWN = 80;
    private static final int TAKE_OFF_TICKS = 40;

    private boolean flying;
    private int takeOffTicks;

    public BlastZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TNT));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FLINT_AND_STEEL));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.moveControl = new BlastMoveControl(this);
        this.goalSelector.addGoal(1, new BlastAttackGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        var followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(96.0);
        }
    }

    public boolean isFlying() {
        return this.flying;
    }

    public void setFlying(boolean flying) {
        if (this.flying != flying) {
            this.flying = flying;
            this.setNoGravity(flying);
            if (flying) {
                this.setSharedFlag(FLAG_FALL_FLYING, true);
                this.takeOffTicks = TAKE_OFF_TICKS;
            } else {
                this.setSharedFlag(FLAG_FALL_FLYING, false);
            }
        }
    }

    @Override
    protected void addBehaviourGoals() {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.takeOffTicks > 0 && --this.takeOffTicks == 0) {
            this.setSharedFlag(FLAG_FALL_FLYING, false);
        }
    }

    private static class BlastMoveControl extends MoveControl {
        private final BlastZombie mob;

        BlastMoveControl(BlastZombie mob) {
            super(mob);
            this.mob = mob;
        }

        @Override
        public void tick() {
            if (!this.mob.isFlying()) {
                super.tick();
                return;
            }
            if (this.operation != Operation.MOVE_TO) {
                mob.setSpeed(0.0F);
                return;
            }
            Vec3 delta = new Vec3(this.getWantedX() - mob.getX(), this.getWantedY() - mob.getY(), this.getWantedZ() - mob.getZ());
            double len = delta.length();
            if (len < 0.6) {
                mob.setSpeed(0.0F);
                mob.setDeltaMovement(Vec3.ZERO);
                return;
            }
            mob.setSpeed(0.8F);
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.85).add(delta.scale(0.05)));
            mob.setYRot(-(float) (Mth.atan2(delta.x, delta.z) * (180.0 / Math.PI)));
            mob.yBodyRot = mob.getYRot();
        }
    }

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
            this.mob.setFlying(false);
            this.mob.setTarget(null);
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            double dSq = this.mob.distanceToSqr(target);
            if (dSq < EXPLODE_RANGE_SQ) {
                this.mob.setFlying(false);
                explodeNearby();
                this.cooldown = (int) ATTACK_COOLDOWN;
            } else if (dSq > WALK_RANGE_SQ) {
                this.mob.setFlying(true);
                this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
            } else {
                this.mob.setFlying(false);
                this.mob.getNavigation().moveTo(target, 1.0);
            }
        }

        private void explodeNearby() {
            ServerLevel level = (ServerLevel) this.mob.level();
            level.sendParticles(ParticleTypes.EXPLOSION,
                    this.mob.getX(), this.mob.getY(), this.mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            for (Player p : level.players()) {
                if (p.distanceToSqr(this.mob) < 36.0) {
                    p.hurtServer(level, level.damageSources().explosion(this.mob, this.mob), 6.0F);
                }
            }
        }
    }
}