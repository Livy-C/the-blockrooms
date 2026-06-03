package name.blockrooms.block.recipe;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Blockrooms.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Blockrooms.MODID);
    public static void register(IEventBus eventBus) { RECIPE_TYPES.register(eventBus); RECIPE_SERIALIZERS.register(eventBus); }

    public static final DeferredHolder<RecipeType<?>, RecipeType<CraftingRecipe>> ERROR_CRAFTING =
            RECIPE_TYPES.register("crafting_error", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Blockrooms.MODID + "crafting_error";
                }
            });
    public static final DeferredHolder<RecipeSerializer<?>, ErrorCraftingRecipe.Serializer> ERROR_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_error", ErrorCraftingRecipe.Serializer::new);
}
