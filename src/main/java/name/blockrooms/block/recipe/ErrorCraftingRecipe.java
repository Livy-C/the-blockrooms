package name.blockrooms.block.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

public class ErrorCraftingRecipe extends ShapedRecipe {
    final ItemStack result;
    final String group;
    final CraftingBookCategory category;
    final boolean showNotification;
    public ErrorCraftingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.group = group;
        this.category = category;
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return super.getSerializer();
    }

    @Override
    public RecipeType<CraftingRecipe> getType() {
        return ModRecipeTypes.ERROR_CRAFTING.get();
    }

    public static class Serializer implements RecipeSerializer<ErrorCraftingRecipe> {
        public static final MapCodec<ErrorCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec((p_340778_) -> p_340778_.group(Codec.STRING.optionalFieldOf("group", "").forGetter((p_311729_) -> p_311729_.group), CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((p_311732_) -> p_311732_.category), ShapedRecipePattern.MAP_CODEC.forGetter((p_311733_) -> p_311733_.pattern), ItemStack.STRICT_CODEC.fieldOf("result").forGetter((p_311730_) -> p_311730_.result), Codec.BOOL.optionalFieldOf("show_notification", true).forGetter((p_311731_) -> p_311731_.showNotification)).apply(p_340778_, ErrorCraftingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingRecipe> STREAM_CODEC = StreamCodec.of(ErrorCraftingRecipe.Serializer::toNetwork, ErrorCraftingRecipe.Serializer::fromNetwork);

        public Serializer() {
        }

        public MapCodec<ErrorCraftingRecipe> codec() {
            return CODEC;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ErrorCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String s = buffer.readUtf();
            CraftingBookCategory category1 = buffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern pattern1 = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack itemstack = ItemStack.STREAM_CODEC.decode(buffer);
            boolean flag = buffer.readBoolean();
            return new ErrorCraftingRecipe(s, category1, pattern1, itemstack, flag);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, ErrorCraftingRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.showNotification);
        }
    }
}
