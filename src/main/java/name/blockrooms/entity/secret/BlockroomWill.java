package name.blockrooms.entity.secret;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 块室意志——秘密空间最终 Boss。
 * 纯自定义 Mob：setNoGravity 悬浮飞行，行为全部手写。
 *
 * 机制：
 * - 10000 血，动态限伤（单次最多 50）
 * - 被 6 条锁链（ChainEntity）束缚：锁链未全断时本体免疫所有伤害
 * - 阶段按已断锁链数推进（0/2/4/5/6），阶段越高攻击越高、技能越多
 * - ServerBossEvent 血条（深紫）
 * - 基础弹幕：凋零骷髅头连发（按阶段解锁环形散射/追踪）
 * - 周期性召唤傀儡（PlayerPuppet / 普通傀儡）
 * - 靠近获得黑暗 + 失明（恐怖压迫感）
 */
public class BlockroomWill extends Monster {

    public static final double MAX_HEALTH = 10000.0D;
    /** 动态限伤：单次受击最多 50 */
    public static final float MAX_HIT_DAMAGE = 50.0F;
    /** 束缚它的锁链数量 */
    public static final int TOTAL_CHAINS = 6;
    /** 体型缩放（巨大化，与旧 SecretEntityOne 一致的 200 倍） */
    public static final double SCALE = 200.0D;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(BlockroomWill.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHAINS_BROKEN = SynchedEntityData.defineId(BlockroomWill.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("块室意志"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    /** 攻击冷却计时 */
    private int attackCooldown;
    /** 召唤冷却计时 */
    private int summonCooldown;
    /** 尖啸计时 */
    private int screamCooldown;
    /** 当前锁链数组 */
    private final java.util.List<UUID> chainIds = new java.util.ArrayList<>();
    /** 已断锁链数 */
    private int chainsBroken;
    /** 是否已生成锁链 */
    private boolean chainsSpawned;

    public BlockroomWill(EntityType<? extends BlockroomWill> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 500;
    }

    /** 生成时应用体型放大（此时属性已注册完毕） */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        var scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(SCALE);
        }
        return data;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.SCALE, SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_CHAINS_BROKEN, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Phase", this.getPhase());
        output.putInt("ChainsBroken", this.chainsBroken);
        output.putBoolean("ChainsSpawned", this.chainsSpawned);
        output.putInt("AttackCooldown", this.attackCooldown);
        output.putInt("SummonCooldown", this.summonCooldown);
        output.putInt("ScreamCooldown", this.screamCooldown);
        output.putInt("ChainCount", this.chainIds.size());
        for (int i = 0; i < this.chainIds.size(); i++) {
            UUID id = this.chainIds.get(i);
            output.putLong("ChainMost" + i, id.getMostSignificantBits());
            output.putLong("ChainLeast" + i, id.getLeastSignificantBits());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setPhase(input.getIntOr("Phase", 0));
        this.chainsBroken = input.getIntOr("ChainsBroken", 0);
        this.chainsSpawned = input.getBooleanOr("ChainsSpawned", false);
        this.attackCooldown = input.getIntOr("AttackCooldown", 0);
        this.summonCooldown = input.getIntOr("SummonCooldown", 0);
        this.screamCooldown = input.getIntOr("ScreamCooldown", 0);
        this.chainIds.clear();
        int count = input.getIntOr("ChainCount", 0);
        for (int i = 0; i < count; i++) {
            long most = input.getLongOr("ChainMost" + i, 0L);
            long least = input.getLongOr("ChainLeast" + i, 0L);
            this.chainIds.add(new UUID(most, least));
        }
    }

    // ================= 阶段与锁链 =================

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    public int getChainsBroken() {
        return this.entityData.get(DATA_CHAINS_BROKEN);
    }

    private void setChainsBroken(int count) {
        this.entityData.set(DATA_CHAINS_BROKEN, count);
    }

    /** 根据已断锁链数计算阶段：0-1 断=1，2-3 断=2，4 断=3，5 断=4，6 断=5 */
    public static int phaseFromChainsBroken(int broken) {
        if (broken >= TOTAL_CHAINS) return 5;
        if (broken >= 5) return 4;
        if (broken >= 4) return 3;
        if (broken >= 2) return 2;
        return 1;
    }

    public boolean isFullyReleased() {
        return this.chainsBroken >= TOTAL_CHAINS;
    }

    public void registerChain(UUID chainId) {
        if (!this.chainIds.contains(chainId)) {
            this.chainIds.add(chainId);
        }
    }

    /** 锁链断裂回调 */
    public void onChainBroken(ChainEntity chain) {
        this.chainsBroken = Math.min(TOTAL_CHAINS, this.chainsBroken + 1);
        this.setChainsBroken(this.chainsBroken);
        this.chainIds.remove(chain.getUUID());
        this.setPhase(phaseFromChainsBroken(this.chainsBroken));
        // 演出：闪电 + 音效
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 0.5F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 3.0F, 1.0F);
        }
        // 召唤一波护卫
        this.summonMinions(2 + this.getPhase());
    }

    /** 生成锁链（由生成逻辑调用）：底部锚点在 Boss 外侧地面，顶部弯曲钩住 Boss 身体中段 */
    public void spawnChains() {
        if (this.chainsSpawned || !(this.level() instanceof ServerLevel serverLevel)) return;
        this.chainsSpawned = true;
        int baseY = this.level().getMinY();
        // Boss 身体半径（200 倍缩放后约 50 格），锁链底部锚点在其外圈
        double anchorRadius = 40.0;
        // 锁链顶部钩在 Boss 身体表面：身体中心偏下（中段）
        double targetRadius = 30.0;
        int targetY = (int) Math.floor(this.getY()) + 15;
        for (int i = 0; i < TOTAL_CHAINS; i++) {
            double angle = Math.PI * 2.0 * i / TOTAL_CHAINS;
            // 底部锚点：Boss 外侧
            double ax = this.getX() + Math.cos(angle) * anchorRadius;
            double az = this.getZ() + Math.sin(angle) * anchorRadius;
            // 顶部钩点：向 Boss 中心收拢（身体表面）
            double tx = this.getX() + Math.cos(angle) * targetRadius;
            double tz = this.getZ() + Math.sin(angle) * targetRadius;

            ChainEntity chain = new ChainEntity(ModSecretEntities.CHAIN.get(), this.level());
            chain.setPos(ax, baseY + 1, az);
            chain.setTopY(targetY);
            chain.setTargetPoint(tx, targetY, tz);
            chain.setOwnerBossId(this.getUUID());
            chain.setHealth(chain.getMaxHealth());
            serverLevel.addFreshEntity(chain);
            this.registerChain(chain.getUUID());
        }
    }

    // ================= 战斗 =================

    /** 阶段攻击力：阶段越高伤害越高 */
    public float getPhaseAttackDamage() {
        return switch (this.getPhase()) {
            case 5 -> 120.0F;
            case 4 -> 90.0F;
            case 3 -> 65.0F;
            case 2 -> 45.0F;
            default -> 30.0F;
        };
    }

    /** 阶段弹幕间隔（tick）：阶段越高越快 */
    private int getPhaseAttackCooldown() {
        return switch (this.getPhase()) {
            case 5 -> 20;
            case 4 -> 30;
            case 3 -> 45;
            case 2 -> 60;
            default -> 80;
        };
    }

    /** 阶段召唤间隔（tick） */
    private int getPhaseSummonCooldown() {
        return switch (this.getPhase()) {
            case 5 -> 120;
            case 4 -> 180;
            case 3 -> 260;
            default -> 400;
        };
    }

    /** 动态限伤 + 锁链无敌（1.21.11 hurt 为 final，用 actuallyHurt 实现） */
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        // 锁链未全断：无敌
        if (!this.isFullyReleased()) return;
        if (this.isInvulnerableTo(level, source)) return;
        // 动态限伤
        float capped = Math.min(amount, MAX_HIT_DAMAGE);
        super.actuallyHurt(level, source, capped);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.3F);
        }
        super.die(damageSource);
    }

    // ================= AI =================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        // 首次 tick 生成锁链
        if (!this.chainsSpawned) {
            this.spawnChains();
        }

        // BossEvent 管理
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.isAlive());
        if (this.isAlive()) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) < 256.0 * 256.0) {
                    this.bossEvent.addPlayer(player);
                } else {
                    this.bossEvent.removePlayer(player);
                }
            }
        } else {
            this.bossEvent.removeAllPlayers();
        }

        // 锁链未全断：被锁住无法移动（但可以攻击/召唤/尖啸）
        boolean bound = !this.isFullyReleased();
        if (bound) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // 无目标：缓慢漂浮，什么都不做
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            // 找最近玩家
            Player nearest = serverLevel.getNearestPlayer(this, 64.0);
            if (nearest != null) {
                this.setTarget(nearest);
            } else {
                this.attackCooldown = Math.max(0, this.attackCooldown - 1);
                this.summonCooldown = Math.max(0, this.summonCooldown - 1);
                return;
            }
        }

        target = this.getTarget();
        if (target == null) return;

        // 悬浮追踪：缓慢飞向目标上方（锁链未断时不动）
        Vec3 toTarget = target.position().subtract(this.position());
        double dist = toTarget.horizontalDistance();
        if (!bound) {
            Vec3 desired = new Vec3(
                    toTarget.x * 0.02,
                    (target.getY() + 6.0 - this.getY()) * 0.05,
                    toTarget.z * 0.02);
            this.setDeltaMovement(desired);
        }
        // 锁定面向目标
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 尖啸：周期性对附近玩家施加黑暗+失明
        this.screamCooldown--;
        if (this.screamCooldown <= 0) {
            this.screamCooldown = 200;
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.3F);
            for (Player p : serverLevel.players()) {
                if (p.distanceToSqr(this) < 32.0 * 32.0) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }
            }
        }

        // 弹幕攻击
        this.attackCooldown--;
        if (this.attackCooldown <= 0 && dist < 48.0) {
            this.attackCooldown = this.getPhaseAttackCooldown();
            this.fireSkulls(target);
        }

        // 召唤
        this.summonCooldown--;
        if (this.summonCooldown <= 0) {
            this.summonCooldown = this.getPhaseSummonCooldown();
            this.summonMinions(1 + this.getPhase() / 2);
        }
    }

    /** 发射凋零骷髅头弹幕 */
    private void fireSkulls(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        int phase = this.getPhase();
        int count = phase >= 3 ? 3 : (phase >= 2 ? 2 : 1);
        Vec3 origin = this.position().add(0, this.getBbHeight() * 0.8, 0);
        for (int i = 0; i < count; i++) {
            // 稍微散布
            Vec3 toTarget = target.position().add(0, 1.0, 0).subtract(origin)
                    .normalize().scale(1.2);
            Vec3 scatter = new Vec3(
                    (this.random.nextDouble() - 0.5) * (phase >= 4 ? 0.4 : 0.2),
                    (this.random.nextDouble() - 0.5) * 0.2,
                    (this.random.nextDouble() - 0.5) * (phase >= 4 ? 0.4 : 0.2));
            WitherSkull skull = new WitherSkull(this.level(), this, toTarget.add(scatter));
            skull.setPos(origin.x, origin.y, origin.z);
            skull.setDangerous(phase >= 4);
            serverLevel.addFreshEntity(skull);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    /** 召唤傀儡护卫 */
    private void summonMinions(int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < count; i++) {
            // 随机选择傀儡类型：PlayerPuppet 或普通傀儡
            EntityType<?> type = this.random.nextBoolean()
                    ? ModSecretEntities.PLAYER_PUPPET.get()
                    : ModSecretEntities.PUPPET.get();
            Entity entity = type.create(serverLevel, EntitySpawnReason.SPAWNER);
            if (entity instanceof Mob mob) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double radius = 3.0 + this.random.nextDouble() * 3.0;
                mob.setPos(this.getX() + Math.cos(angle) * radius, this.getY() - 2, this.getZ() + Math.sin(angle) * radius);
                mob.setTarget(this.getTarget());
                serverLevel.addFreshEntity(mob);
            }
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }
}
