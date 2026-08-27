package name.blockrooms.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


public class SculkCreeper extends Creeper {
    private static final double ATTACK_RANGE = 6.0;
    private static final double EXPLOSION_RANGE = 8.0;
    private static final int EXPLODE_COOLDOWN = 120;

    public SculkCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(80.0);
        }
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(1, new SculkBlastGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, new Class[0]));
    }

    @Override
    protected void explodeCreeper() {
    }

    private void explodeNow() {
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        for (Player p : level.players()) {
            double d = p.distanceToSqr(this);
            if (d < EXPLOSION_RANGE * EXPLOSION_RANGE) {
                Vec3 vec3 = p.position();
                Vec3 vec31 = this.getEyePosition().subtract(vec3);
                Vec3 vec32 = vec31.normalize();
                p.hurtServer(level, level.damageSources().explosion(this, this), 15.0F);
                int i = Mth.floor(vec31.length()) + 7;
                for (int j = 1; j < i; ++j) {
                    Vec3 vec33 = vec3.add(vec32.scale(j));
                    level.sendParticles(ParticleTypes.SONIC_BOOM, vec33.x, vec33.y, vec33.z, 1, 0.0F, 0.0F, 0.0F, 0.0F);
                }
            }
        }
        this.discard();
    }

    private static class SculkBlastGoal extends Goal {
        private static final int FUSE_TICKS = 40;
        private static final int COOLDOWN_TICKS = 80;

        private final SculkCreeper mob;
        private int fuse;
        private int cooldown;

        SculkBlastGoal(SculkCreeper mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target instanceof Player && target.isAlive() && --this.cooldown <= 0
                    && this.mob.distanceToSqr(target) < ATTACK_RANGE * ATTACK_RANGE;
        }

        @Override
        public void start() {
            this.mob.setSwellDir(1);
            this.fuse = FUSE_TICKS;
        }

        @Override
        public void stop() {
            this.mob.setSwellDir(-1);
            this.cooldown = COOLDOWN_TICKS;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null || !target.isAlive() || this.mob.distanceToSqr(target) > 64.0) {
                this.mob.setSwellDir(-1);
                this.cooldown = COOLDOWN_TICKS;
                return;
            }
            if (--this.fuse <= 0) {
                this.mob.explodeNow();
            }
        }
    }
}