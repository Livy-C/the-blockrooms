package name.blockrooms.world.secret;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class SecretPlaceAccess {
    public static final Identifier SECRET_PLACE_ID = Identifier.fromNamespaceAndPath("blockrooms", "secret_place");
    public static final ResourceKey<Level> SECRET_PLACE_KEY = ResourceKey.create(Registries.DIMENSION, SECRET_PLACE_ID);
    private static final String UNLOCKED_TAG = "blockrooms.secret_place_unlocked";

    private SecretPlaceAccess() {
    }

    public static boolean isUnlocked(ServerPlayer player) {
        return player.getPersistentData().getBooleanOr(UNLOCKED_TAG, false);
    }

    public static void unlock(ServerPlayer player) {
        player.getPersistentData().putBoolean(UNLOCKED_TAG, true);
    }

    public static boolean isSecretPlace(Identifier id) {
        return id.equals(SECRET_PLACE_ID);
    }
}