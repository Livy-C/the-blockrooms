package name.blockrooms.block;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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
            BLOCKS.registerBlock("stone_crafting_table", BaseCraftingTableBlock::new,
                    properties -> properties.mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F));
    public static final DeferredBlock<DetectorTorchBlock> DETECTOR_TORCH =
            BLOCKS.registerBlock("detector_torch",
                    properties -> new DetectorTorchBlock(ParticleTypes.FLAME, properties),
                    properties -> properties.noCollision()
                            .instabreak()
                            .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)
                            .sound(SoundType.WOOD)
                            .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<DetectorWallTorchBlock> DETECTOR_WALL_TORCH =
            BLOCKS.registerBlock("detector_wall_torch",
                    properties -> new DetectorWallTorchBlock(ParticleTypes.FLAME, properties),
                    properties -> properties
                            .overrideLootTable(DETECTOR_TORCH.get().getLootTable())
                            .overrideDescription(DETECTOR_TORCH.get().getDescriptionId())
                            .noCollision()
                            .instabreak()
                            .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)
                            .sound(SoundType.WOOD)
                            .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<DetectorRedstoneLampBlock> DETECTOR_REDSTONE_LAMP_BLOCK =
            BLOCKS.registerBlock("detector_redstone_lamp", DetectorRedstoneLampBlock::new,
            properties -> properties
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .isValidSpawn(Blocks::always));
    public static final DeferredBlock<Block> QUARTZ_ELEVATOR = BLOCKS.registerSimpleBlock(
            "quartz_elevator",
            properties -> properties.mapColor(MapColor.QUARTZ)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.8F));
}
