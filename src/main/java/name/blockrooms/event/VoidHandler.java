package name.blockrooms.event;

import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class VoidHandler {
    @SubscribeEvent
    public static void onEntityTick(PlayerTickEvent.Pre event) {
        if(event.getEntity() instanceof ServerPlayer sp){
            if(ModLevels.isInBlockrooms(sp.level().dimension())){
                if(sp.getY() < sp.level().getMinY() - 32){
                    if(!sp.level().dimension().equals(ModLevels.BLOCKLEVEL_NULL)) TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_NULL);
                    else{
                        if(sp.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)){
                            TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_1);
                        } else {
                            TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_0);
                        }
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerDeath(PlayerRespawnPositionEvent event){
        if(event.getEntity() instanceof ServerPlayer p){
            if(ModLevels.isInBlockrooms(p.level().dimension())){
                var c = event.getTeleportTransition();
                event.setTeleportTransition(c.withPosition(new Vec3(0, 1, 0)));
                event.setRespawnLevel(ModLevels.BLOCKLEVEL_NULL);
            }
        }
    }
}
