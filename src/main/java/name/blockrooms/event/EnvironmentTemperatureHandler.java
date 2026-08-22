package name.blockrooms.event;

import name.blockrooms.environment.EnvironmentTemperatureModule;
import name.blockrooms.item.HeatInsulationArmor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 环境温度 + 隔热套装的挂载点：把模块接入玩家 tick。
 */
@EventBusSubscriber
public class EnvironmentTemperatureHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EnvironmentTemperatureModule.onPlayerTick(player);
        if (player.tickCount % 20 == 0) {
            HeatInsulationArmor.APPLY_PASSIVE_EFFECT.accept(player);
        }
    }
}
