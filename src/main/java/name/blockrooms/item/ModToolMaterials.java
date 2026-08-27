package name.blockrooms.item;

import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterials {
    public static final ToolMaterial RUBY = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS
    );
    public static final ToolMaterial STRING = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 16.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS
    );
    public static final ToolMaterial SOUL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 780, 8.0F, 2.5F, 10, repairsSoulAlloy()
    );

    public static TagKey<Item> repairsSoulAlloy() {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                Identifier.fromNamespaceAndPath(Blockrooms.MODID, "repairs_soul_alloy"));
    }
}