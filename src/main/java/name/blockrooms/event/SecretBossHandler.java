package name.blockrooms.event;

import name.blockrooms.entity.secret.BlockroomWill;
import name.blockrooms.entity.secret.ModSecretEntities;
import name.blockrooms.world.secret.SecretPlaceAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * 玩家进入秘密空间时生成"块室意志"Boss（若尚未存在）。
 */
@EventBusSubscriber
public class SecretBossHandler {

    /** Boss 生成位置（秘密空间原点附近） */
    private static final BlockPos BOSS_SPAWN = new BlockPos(8, 40, 8);

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.getDimension().equals(SecretPlaceAccess.SECRET_PLACE_KEY)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.level().getServer().getLevel(SecretPlaceAccess.SECRET_PLACE_KEY);
        if (level == null) return;

        // 已存在 Boss 则不重复生成
        boolean exists = !level.getEntitiesOfClass(BlockroomWill.class, 
                new net.minecraft.world.phys.AABB(BOSS_SPAWN).inflate(256.0)).isEmpty();
        if (exists) return;

        BlockroomWill boss = new BlockroomWill(ModSecretEntities.BLOCKROOM_WILL.get(), level);
        boss.setPos(BOSS_SPAWN.getX() + 0.5, BOSS_SPAWN.getY(), BOSS_SPAWN.getZ() + 0.5);
        boss.setHealth(boss.getMaxHealth());
        level.addFreshEntity(boss);
        boss.spawnChains();
    }
}
