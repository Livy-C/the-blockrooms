package name.blockrooms.block.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public class ErrorCraftingShapedRecipe extends ShapedRecipe implements ErrorCraftingRecipe {
    final ItemStack result;

    public ErrorCraftingShapedRecipe(ShapedRecipePattern pattern, ItemStack result) {
        super("error", CraftingBookCategory.MISC, pattern, result, false);
        this.result = result;
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return ModRecipeTypes.ERROR_CRAFTING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<ErrorCraftingShapedRecipe> {
        public static final MapCodec<ErrorCraftingShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(o -> o.result)
                ).apply(i, ErrorCraftingShapedRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapedRecipe> STREAM_CODEC =
                StreamCodec.of(ErrorCraftingShapedRecipe.Serializer::toNetwork, ErrorCraftingShapedRecipe.Serializer::fromNetwork);

        public Serializer() {
        }

        public MapCodec<ErrorCraftingShapedRecipe> codec() {
            return CODEC;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ErrorCraftingShapedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new ErrorCraftingShapedRecipe(pattern, result);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, ErrorCraftingShapedRecipe recipe) {
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }
}
