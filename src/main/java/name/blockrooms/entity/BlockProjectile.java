package name.blockrooms.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockProjectile extends Projectile {
    public BlockProjectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    private BlockProjectile(Level level, Entity owner, BlockState blockState){
        super(ModEntities.BLOCK_PROJECTILE.get(), level);
        this.setBlockState(blockState);
        this.setOwner(owner);
    }

    public static BlockProjectile of(Level level, Entity owner, BlockState state){
        return new BlockProjectile(level, owner, state);
    }

    public static final String TAG_BLOCK_STATE = "block_state";
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(
            BlockProjectile.class, EntityDataSerializers.BLOCK_STATE
    );
    private BlockRenderState blockRenderState;

    private boolean updateRenderState = false;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326452_) {
        p_326452_.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_277476_) {
        super.onSyncedDataUpdated(p_277476_);
        if (p_277476_.equals(DATA_BLOCK_STATE_ID)) {
            this.updateRenderState = true;
        }
    }
    protected void updateRenderSubState() {
        this.blockRenderState = new BlockRenderState(this.getBlockState());
    }
    private BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE_ID);
    }

    private void setBlockState(BlockState blockState) {
        this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_422259_) {
        super.readAdditionalSaveData(p_422259_);
        this.setBlockState(p_422259_.read(TAG_BLOCK_STATE, BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_422670_) {
        super.addAdditionalSaveData(p_422670_);
        p_422670_.store(TAG_BLOCK_STATE, BlockState.CODEC, this.getBlockState());
    }

    public BlockRenderState blockRenderState() {
        return this.blockRenderState;
    }

    public record BlockRenderState(BlockState blockState) {
    }
}
