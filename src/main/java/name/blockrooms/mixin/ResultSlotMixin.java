package name.blockrooms.mixin;

import name.blockrooms.block.inventory.ErrorCraftingMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Accessor("player")
    public abstract Player getPlayer();

    @Inject(method = "checkTakeAchievements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/RecipeCraftingHolder;awardUsedRecipes(Lnet/minecraft/world/entity/player/Player;Ljava/util/List;)V"
            ),
            cancellable = true,
            remap = false)
    private void cancelErrorCraftingRecipeAward(ItemStack stack, CallbackInfo ci) {
        Player player = this.getPlayer();
        if (player != null && player.containerMenu instanceof ErrorCraftingMenu) {
            ci.cancel();
        }
    }
}
