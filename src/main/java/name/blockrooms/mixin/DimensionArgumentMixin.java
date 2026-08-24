package name.blockrooms.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import name.blockrooms.world.secret.SecretPlaceAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionArgument.class)
public class DimensionArgumentMixin {
    @Unique
    private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("argument.dimension.invalid", id));

    @Inject(method = "getDimension", at = @At("HEAD"))
    private static void blockrooms$hideSecretPlace(CommandContext<CommandSourceStack> context, String name,
                                                   CallbackInfoReturnable<ServerLevel> cir) throws CommandSyntaxException {
        Identifier id = context.getArgument(name, Identifier.class);
        if (!SecretPlaceAccess.isSecretPlace(id)) {
            return;
        }
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null || !SecretPlaceAccess.isUnlocked(player)) {
            throw ERROR_INVALID_VALUE.create(id);
        }
    }
}
