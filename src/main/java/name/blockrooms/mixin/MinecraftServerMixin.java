package name.blockrooms.mixin;

import name.blockrooms.world.secret.SecretPlaceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    private PlayerList playerList;

    @Inject(method = "levelKeys", at = @At("HEAD"), cancellable = true)
    private void blockrooms$hideSecretPlaceKey(CallbackInfoReturnable<Set<ResourceKey<Level>>> cir) {
        if (blockrooms$noOneUnlocked()) {

            cir.setReturnValue(this.levels.keySet().stream()
                    .filter(key -> !SecretPlaceAccess.isSecretPlace(key.identifier()))
                    .collect(Collectors.toSet()));
        }
    }

    @Inject(method = "getAllLevels", at = @At("HEAD"), cancellable = true)
    private void blockrooms$hideSecretPlaceFromLevels(CallbackInfoReturnable<Iterable<ServerLevel>> cir) {
        if (blockrooms$noOneUnlocked()) {
            cir.setReturnValue(List.copyOf(this.levels.values().stream()
                    .filter(level -> !SecretPlaceAccess.isSecretPlace(level.dimension().identifier()))
                    .toList()));
        }
    }

    @Unique
    private boolean blockrooms$noOneUnlocked() {
        return this.playerList == null
                || this.playerList.getPlayers().stream().noneMatch(SecretPlaceAccess::isUnlocked);
    }
}