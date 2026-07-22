package name.blockrooms.event;

import name.blockrooms.client.key.ModKeyBindings;
import name.blockrooms.network.ElevatorTeleportPayload;
import name.blockrooms.network.NoclipPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class ModKeyHandler {
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (ModKeyBindings.noclippingKey.consumeClick()) {
            ClientPacketDistributor.sendToServer(new NoclipPayload());
        }
        if (Minecraft.getInstance().options.keyJump.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ElevatorTeleportPayload(true));
        }
        if (Minecraft.getInstance().options.keyShift.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ElevatorTeleportPayload(false));
        }
    }
}
