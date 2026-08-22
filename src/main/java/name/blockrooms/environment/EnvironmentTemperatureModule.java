package name.blockrooms.environment;

import name.blockrooms.item.HeatInsulationArmor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;
import java.util.function.Predicate;


public final class EnvironmentTemperatureModule {
    /** 灼热阈值：生物群系温度高于该值开始扣血（沙漠/恶地/下界等均为 2.0） */
    public static final float DAMAGE_THRESHOLD = 2.0F;
    /** 每次造成的伤害（1 = 半颗心） */
    public static final float DAMAGE_PER_HIT = 1.0F;
    /** 检查间隔（游戏刻） */
    public static final int CHECK_INTERVAL = 20;
    /** 每穿一件隔热装备减免的伤害比例 */
    public static final float PROTECTION_PER_PIECE = 0.25F;

    public static final Predicate<ServerPlayer> CAN_BE_HURT = player ->
            !player.isCreative() && !player.isSpectator() && !player.isDeadOrDying() && !player.isInvulnerable();

    public static final Predicate<ServerPlayer> IS_OVERHEATED = player ->
            temperatureAt(player) > DAMAGE_THRESHOLD;

    public static final Consumer<ServerPlayer> APPLY_HEAT_DAMAGE = player -> {
        int worn = HeatInsulationArmor.wornCount(player);
        float multiplier = Math.max(0.0F, 1.0F - PROTECTION_PER_PIECE * worn);
        if (multiplier > 0.0F) {
            player.hurt(player.damageSources().onFire(), DAMAGE_PER_HIT * multiplier);
        }
    };

    public static float temperatureAt(Entity entity) {
        return entity.level().getBiome(entity.blockPosition()).value().getBaseTemperature();
    }

    /** 每个游戏刻调用：先由 Predicate 组合判定，命中后交给 Consumer 执行 */
    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        if (CAN_BE_HURT.and(IS_OVERHEATED).test(player)) {
            APPLY_HEAT_DAMAGE.accept(player);
        }
    }

    private EnvironmentTemperatureModule() {
    }
}
