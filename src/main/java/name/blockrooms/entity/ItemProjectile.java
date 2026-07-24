package name.blockrooms.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemProjectile extends Projectile {
    protected boolean updateRenderState;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
            ItemProjectile.class, EntityDataSerializers.ITEM_STACK
    ); private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(ItemProjectile.class, EntityDataSerializers.BYTE);
    private ItemRenderState itemRenderState;
    private ItemStack getItemStack() {
        return this.entityData.get(DATA_ITEM_STACK_ID);
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_277793_) {
        super.onSyncedDataUpdated(p_277793_);
        if (DATA_ITEM_STACK_ID.equals(p_277793_) || DATA_ITEM_DISPLAY_ID.equals(p_277793_)) {
            this.updateRenderState = true;
        }
    }
    protected ItemProjectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
    }
    public ItemRenderState itemRenderState() {
        return this.itemRenderState;
    }
    private void setItemTransform(ItemDisplayContext itemTransform) {
        this.entityData.set(DATA_ITEM_DISPLAY_ID, itemTransform.getId());
    }

    private ItemDisplayContext getItemTransform() {
        return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
    }
    @Override
    public void tick() {
        super.tick();
        if(updateRenderState)
            this.updateRenderSubState();
    }

    protected void updateRenderSubState() {
        ItemStack itemstack = this.getItemStack();
        itemstack.setEntityRepresentation(this);
        this.itemRenderState = new ItemRenderState(itemstack, getItemTransform());
    }
    public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
    }
}
