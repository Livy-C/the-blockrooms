package name.blockrooms;

import name.blockrooms.client.hud.DifficultyLayer;
import name.blockrooms.client.hud.LevelInfoLayer;
import name.blockrooms.client.hud.TemperatureSensorLayer;
import name.blockrooms.client.renderer.BlackstoneShulkerRenderer;
import name.blockrooms.client.renderer.BlockProjectileRenderer;
import name.blockrooms.client.renderer.BloodZombieRenderer;
import name.blockrooms.client.renderer.ItemProjectileRenderer;
import name.blockrooms.entity.ModEntities;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(value = Blockrooms.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Blockrooms.MODID, value = Dist.CLIENT)
public class BlockroomsClient {
    public BlockroomsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLOCK_PROJECTILE.get(), BlockProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.ITEM_PROJECTILE.get(), ItemProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_ZOMBIE.get(), BloodZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACKSTONE_SHULKER.get(), BlackstoneShulkerRenderer::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Blockrooms.MODID, "level_info"),
                LevelInfoLayer.instance());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Blockrooms.MODID, "level_difficulty"),
                DifficultyLayer.instance());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Blockrooms.MODID, "temperature_sensor"),
                TemperatureSensorLayer.instance());
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        // Call event.createDatapackRegistryObjects(...) first if adding datapack objects
    }
}
