package name.blockrooms.network;

import name.blockrooms.event.NoclipHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handleNoclipping(final NoclipPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                player.causeFoodExhaustion(3);
                NoclipHandler.noclipByCondition(player, player.getBlockStateOn());
            }
                //player.hurtServer(player.level(), player.damageSources().inWall(), 3);
        });
    }
}
