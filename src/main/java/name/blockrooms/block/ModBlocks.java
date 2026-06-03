package name.blockrooms.block;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.inventory.BaseCraftingMenu;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Blockrooms.MODID);
    public static void register(IEventBus eventBus) { BLOCKS.register(eventBus); }

    public static final DeferredBlock<Block> HEATED_IRON_BLOCK = BLOCKS.registerSimpleBlock(
            "heated_iron_block",
            properties -> properties.instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .mapColor(MapColor.FIRE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.IRON)
    );
    public static final DeferredBlock<BaseCraftingTableBlock> ERROR_CRAFTING_TABLE =
            BLOCKS.registerBlock("error_crafting_table",
                    properties -> new BaseCraftingTableBlock(
                            Component.translatable("container.error_crafting").withColor(ChatFormatting.RED.getColor()),
                            ErrorCraftingMenu::new, properties),
                    properties -> properties.mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(2.5F).sound(SoundType.WOOD)
                            .ignitedByLava());
    public static final DeferredBlock<BaseCraftingTableBlock> STONE_CRAFTING_TABLE =
            BLOCKS.registerBlock("stone_crafting_table",
                    properties -> new BaseCraftingTableBlock(BaseCraftingMenu::new, properties),
                    properties -> properties.mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F));
}
