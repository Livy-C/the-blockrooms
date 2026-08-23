package name.blockrooms.block.recipe;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

public interface ErrorCraftingRecipe extends CraftingRecipe {
    @Override
    default RecipeType<CraftingRecipe> getType() {
        return ModRecipeTypes.ERROR_CRAFTING.get();
    }
}
