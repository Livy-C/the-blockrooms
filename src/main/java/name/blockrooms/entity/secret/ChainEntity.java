package name.blockrooms.entity.secret;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;


public class ChainEntity extends Mob {
    public static final float MAX_HIT_DAMAGE = 100.0F;
    public static final double MAX_HEALTH = 2000.0D;

    private static final EntityDataAccessor<Float> DATA_TARGET_X = SynchedEntityData.defineId(ChainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z = SynchedEntityData.defineId(ChainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_Y = SynchedEntityData.defineId(ChainEntity.class, EntityDataSerializers.INT);

    /** 锁链顶端的 Y 坐标（从基岩到的高度） */
    private int topY;
    /** 归属的 Boss UUID */
    private java.util.UUID ownerBossId;
    /** 链长（渲染用） */
    private int chainLength = 32;

    public ChainEntity(EntityType<? extends ChainEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET_X, 0.0F);
        builder.define(DATA_TARGET_Z, 0.0F);
        builder.define(DATA_TARGET_Y, 0);
    }

    /** 设置锁链顶部要钩住的目标点（Boss 身体位置），用于渲染弯曲 */
    public void setTargetPoint(double x, double y, double z) {
        this.entityData.set(DATA_TARGET_X, (float) x);
        this.entityData.set(DATA_TARGET_Y, (int) y);
        this.entityData.set(DATA_TARGET_Z, (float) z);
    }

    public double getTargetX() {
        return this.entityData.get(DATA_TARGET_X);
    }

    public double getTargetZ() {
        return this.entityData.get(DATA_TARGET_Z);
    }

    public int getTargetY() {
        return this.entityData.get(DATA_TARGET_Y);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TopY", this.topY);
        output.putInt("ChainLength", this.chainLength);
        if (this.ownerBossId != null) {
            output.putInt("OwnerBossMost", (int) (this.ownerBossId.getMostSignificantBits() >> 32));
            output.putInt("OwnerBossLeast", (int) this.ownerBossId.getMostSignificantBits());
            output.putInt("OwnerBossL", (int) (this.ownerBossId.getLeastSignificantBits() >> 32));
            output.putInt("OwnerBossLL", (int) this.ownerBossId.getLeastSignificantBits());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.topY = input.getIntOr("TopY", 0);
        this.chainLength = input.getIntOr("ChainLength", 32);
        if (input.getInt("OwnerBossMost").isPresent()) {
            long most = ((long) input.getIntOr("OwnerBossMost", 0) << 32) | (input.getIntOr("OwnerBossLeast", 0) & 0xFFFFFFFFL);
            long least = ((long) input.getIntOr("OwnerBossL", 0) << 32) | (input.getIntOr("OwnerBossLL", 0) & 0xFFFFFFFFL);
            this.ownerBossId = new java.util.UUID(most, least);
        }
    }

    public void setChainLength(int length) {
        this.chainLength = length;
    }

    public int getChainLength() {
        return this.chainLength;
    }

    public void setTopY(int y) {
        this.topY = y;
        this.setChainLength(Math.max(1, y - this.blockPosition().getY()));
    }

    public int getTopY() {
        return this.topY;
    }

    public void setOwnerBossId(java.util.UUID id) {
        this.ownerBossId = id;
    }

    public java.util.UUID getOwnerBossId() {
        return this.ownerBossId;
    }
    
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        float capped = Math.min(amount, MAX_HIT_DAMAGE);
        super.actuallyHurt(level, source, capped);
        if (this.isAlive()) {
            level.playSound(null, this.blockPosition(), SoundEvents.CHAIN_BREAK, this.getSoundSource(), 1.0F, 0.5F + this.random.nextFloat() * 0.5F);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel && this.ownerBossId != null) {
            if (serverLevel.getEntity(this.ownerBossId) instanceof BlockroomWill boss) {
                boss.onChainBroken(this);
            }
        }
        super.die(damageSource);
    }
    
    
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.setDeltaMovement(Vec3.ZERO);
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
}
