package name.blockrooms.world.secret;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 秘密地点（secret_place 维度）的解锁访问控制（Mixin 方案，按玩家 NBT）。
 * <p>
 * 维度 JSON 打包在模组数据包中，Level 启动时即创建；未解锁时由
 * {@code DimensionArgumentMixin}（指令）与 {@code SecretPlaceAccessHandler}（传送）拦截，
 * 使该维度对<b>未解锁玩家</b>的一切指令（/execute in、/tp 等）与传送不可见。
 * 解锁 = 在玩家 NBT 上写入 {@code unlocked} 标记（{@link #unlock}），<b>立即生效，无需重启</b>。
 */
public final class SecretPlaceAccess {
    /** 维度 id：blockrooms:secret_place */
    public static final Identifier SECRET_PLACE_ID = Identifier.fromNamespaceAndPath("blockrooms", "secret_place");
    /** 维度 key */
    public static final ResourceKey<Level> SECRET_PLACE_KEY = ResourceKey.create(Registries.DIMENSION, SECRET_PLACE_ID);
    /** 玩家 NBT 解锁标记 */
    private static final String UNLOCKED_TAG = "blockrooms.secret_place_unlocked";

    private SecretPlaceAccess() {
    }

    /** 该玩家是否已解锁 */
    public static boolean isUnlocked(ServerPlayer player) {
        return player.getPersistentData().getBooleanOr(UNLOCKED_TAG, false);
    }

    /** 解锁该玩家（写入 NBT，立即生效） */
    public static void unlock(ServerPlayer player) {
        player.getPersistentData().putBoolean(UNLOCKED_TAG, true);
    }

    /** 是否为 secret_place 维度 id（隐藏判定用） */
    public static boolean isSecretPlace(Identifier id) {
        return id.equals(SECRET_PLACE_ID);
    }
}
