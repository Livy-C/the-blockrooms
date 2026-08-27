package name.blockrooms.event;

import name.blockrooms.world.secret.SecretPlaceAccess;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

@EventBusSubscriber
public class SecretPlaceAccessHandler {
    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.getDimension().equals(SecretPlaceAccess.SECRET_PLACE_KEY)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && !SecretPlaceAccess.isUnlocked(player)) {
            event.setCanceled(true);
        }
    }
}