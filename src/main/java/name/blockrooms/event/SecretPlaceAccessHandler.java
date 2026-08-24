package name.blockrooms.event;

import name.blockrooms.world.secret.SecretPlaceAccess;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * 秘密地点传送拦截：未解锁玩家通过任何途径（指令以外的传送代码等）
 * 尝试进入 secret_place 维度时被取消。
 */
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
