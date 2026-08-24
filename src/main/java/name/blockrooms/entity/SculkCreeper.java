package name.blockrooms.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 幽匿苦力怕：BlockLevel 13.8 的苦力怕变种。
 * <ul>
 *   <li>生命值为普通苦力怕的 <b>4 倍</b>（80）；</li>
 *   <li>不可用打火石引爆（引信 AI 已移除）；</li>
 *   <li>锁定玩家后靠近 <b>隔方块引爆</b>：AoE 爆炸伤害无视遮挡 + 监守者声波粒子；</li>
 *   <li>爆炸不破坏地形。</li>
 * </ul>
 */
public class SculkCreeper extends Creeper {
    private static final double ATTACK_RANGE = 6.0;
    private static final double EXPLOSION_RANGE = 8.0;
    private static final int EXPLODE_COOLDOWN = 120;

    public SculkCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(80.0); // 4 倍血量
        }
        // 替换 AI：自动接近玩家引爆（原版引信不可用）
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(1, new SculkBlastGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 引爆：AoE 爆炸伤害（无视遮挡）+ 监守者声波粒子，不破坏地形，引爆后消失 */
    private void explodeNow() {
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        for (Player p : level.players()) {
            double d = p.distanceToSqr(this);
            if (d < EXPLOSION_RANGE * EXPLOSION_RANGE) {
                Vec3 dir = p.position().subtract(this.position()).normalize();
                level.sendParticles(ParticleTypes.SONIC_BOOM,
                        this.getX(), this.getY() + 1.0, this.getZ(), 1, dir.x, dir.y, dir.z, 0.0);
                p.hurtServer(level, level.damageSources().explosion(this, this), 15.0F);
            }
        }
        this.discard();
    }

    private class SculkBlastGoal extends Goal {
        private final SculkCreeper mob;
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
            this.cooldown = EXPLODE_COOLDOWN;
        }

        @Override
        public void tick() {
            this.mob.explodeNow();
        }
    }
}
