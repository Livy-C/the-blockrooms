package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * BL542 时间/天气控制：时间固定午夜（18000），天气固定雷雨（不停止）。
 */
@EventBusSubscriber
public class BlockLevel542Handler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(ModLevels.BLOCKLEVEL_542);
        if (level == null) {
            return;
        }
        // 时间固定午夜（18000 tick = 0:00）
        level.setDayTime(18000);
        // 永久雷雨：清空计时器、强制下雨 + 打雷
        level.setWeatherParameters(0, 0, true, true);
    }
}
