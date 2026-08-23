package name.blockrooms.block.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ErrorCraftingShapelessRecipe extends ShapelessRecipe implements ErrorCraftingRecipe {
    final ItemStack result;
    final List<Ingredient> ingredients;
    private @Nullable PlacementInfo placementInfo;
    private final boolean isSimple;

    public ErrorCraftingShapelessRecipe(ItemStack result, List<Ingredient> ingredients) {
        super("error", CraftingBookCategory.MISC, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) ModRecipeTypes.ERROR_CRAFTING_SHAPELESS_SERIALIZER.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.create(this.ingredients);
        }

        return this.placementInfo;
    }

    public boolean matches(CraftingInput craftingInput, Level level) {
        if (craftingInput.ingredientCount() != this.ingredients.size()) {
            return false;
        } else if (!isSimple) {
            var nonEmptyItems = new java.util.ArrayList<ItemStack>(craftingInput.ingredientCount());
            for (var item : craftingInput.items())
                if (!item.isEmpty())
                    nonEmptyItems.add(item);
            return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(nonEmptyItems, this.ingredients) != null;
        } else {
            return craftingInput.size() == 1 && this.ingredients.size() == 1
                    ? this.ingredients.getFirst().test(craftingInput.getItem(0))
                    : craftingInput.stackedContents().canCraft(this, null);
        }
    }

    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider) {
        return this.result.copy();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new ShapelessCraftingRecipeDisplay(
                        this.ingredients.stream().map(Ingredient::display).toList(),
                        new SlotDisplay.ItemStackSlotDisplay(this.result),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
                )
        );
    }

    public static class Serializer implements RecipeSerializer<ErrorCraftingShapelessRecipe> {
        private static final MapCodec<ErrorCraftingShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_301142_ -> p_301142_.result),
                                Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, ShapedRecipePattern.getMaxHeight() * ShapedRecipePattern.getMaxWidth())).fieldOf("ingredients").forGetter(p_360071_ -> p_360071_.ingredients)
                        )
                        .apply(i, ErrorCraftingShapelessRecipe::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, o -> o.result,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
                ErrorCraftingShapelessRecipe::new
        );

        @Override
        public MapCodec<ErrorCraftingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
