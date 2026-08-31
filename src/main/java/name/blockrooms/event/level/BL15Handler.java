package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import name.blockrooms.world.generator.BlockLevel15Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class BL15Handler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel sl : event.getServer().getAllLevels()) {
            if (!sl.dimension().equals(ModLevels.BLOCKLEVEL_15)) {
                continue;
            }
            if (!sl.isRaining()) {
                sl.setWeatherParameters(0, 24000, true, false);
            }
            if (sl.getDayTime() % 24000 != 6000) {
                sl.setDayTime(6000);
            }
        }
    }

    /**
     * 拦截 BL15 → 末地的维度传送（原版 END_PORTAL 方块踩上即触发）。
     * BL15 的末地传送门实际应把玩家送到另一座拱门顶部，
     * 因此取消原版传送并手动传送到目标拱门。
     */
    @SubscribeEvent
    public static void onTravelToEnd(EntityTravelToDimensionEvent event) {
        // 只拦截"传送去末地"的情况
        if (!event.getDimension().equals(ServerLevel.END)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_15)) {
            return;
        }
        // 取消原版末地传送
        event.setCanceled(true);
        // 找到玩家脚下的传送门方块（容忍 1 格偏差）
        BlockPos portalPos = player.blockPosition().below();
        if (!level.getBlockState(portalPos).is(Blocks.END_PORTAL)) {
            portalPos = player.blockPosition();
        }
        Vec3 target = portalTarget(level.getSeed(), portalPos);
        player.teleportTo(level, target.x, target.y, target.z,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.level();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_15)) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        if (level.getBlockState(below).is(Blocks.END_PORTAL)) {
            Vec3 target = portalTarget(level.getSeed(), below);
            player.teleportTo(level, target.x, target.y, target.z,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        }
    }

    public static Vec3 portalTarget(long seed, BlockPos portalPos) {
        int gx0 = Math.floorDiv(portalPos.getX(), BlockLevel15Generator.ARCH_GRID);
        int gz0 = Math.floorDiv(portalPos.getZ(), BlockLevel15Generator.ARCH_GRID);
        long h = seed ^ (portalPos.getX() * 0x9E3779B97F4A7C15L) ^ (portalPos.getZ() * 0xBF58476D1CE4E5B9L) ^ 0x15B15L;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 32)) & Long.MAX_VALUE;
        int dx = 2 + (int) (h % 6);
        int dz = 2 + (int) ((h >>> 8) % 6);
        if ((h & 1) == 0) dx = -dx; else dz = -dz;
        int gx = gx0 + dx;
        int gz = gz0 + dz;
        if (!BlockLevel15Generator.archExists(seed, gx, gz)) {
            gx = gx0 - dx;
            gz = gz0 - dz;
        }
        BlockPos midTop = BlockLevel15Generator.archMidTop(seed, gx, gz);
        return new Vec3(midTop.getX() + 0.5, midTop.getY(), midTop.getZ() + 0.5);
    }

    public static void onPlayerTransform(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getFrom().equals(ModLevels.BLOCKLEVEL_15) && event.getFrom().equals(ServerLevel.END)){

        }
    }
}