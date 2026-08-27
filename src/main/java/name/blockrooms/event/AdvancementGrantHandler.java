package name.blockrooms.event;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import name.blockrooms.util.ModLevels;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber
public class AdvancementGrantHandler {

    private static final ResourceKey<Advancement> ERROR_CRAFTING_TABLE = advancement("error_crafting_table");
    private static final ResourceKey<Advancement> LIGHTS_OUT = advancement("lights_out");

    private static ResourceKey<Advancement> advancement(String path) {
        return ResourceKey.create(Registries.ADVANCEMENT, Identifier.fromNamespaceAndPath(Blockrooms.MODID, path));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getContainer() instanceof ErrorCraftingMenu) {
            grant(player, ERROR_CRAFTING_TABLE, "opened");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var level = player.level();
        if (!level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        if (level.getDayTime() % 24000 < 13000) {
            return;
        }
        grant(player, LIGHTS_OUT, "night");
    }

    private static void grant(ServerPlayer player, ResourceKey<Advancement> key, String criterion) {
        if (player.level() instanceof ServerLevel serverLevel) {
            AdvancementHolder holder = serverLevel.getServer().getAdvancements().get(key.identifier());
            if (holder != null) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }
}