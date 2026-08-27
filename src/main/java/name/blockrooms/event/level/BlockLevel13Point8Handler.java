package name.blockrooms.event.level;

import io.netty.buffer.Unpooled;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.entity.BlastZombie;
import name.blockrooms.entity.ModEntities;
import name.blockrooms.entity.SculkCreeper;
import name.blockrooms.util.ModLevels;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class BlockLevel13Point8Handler {
    private static final String SAVED_INVENTORY_TAG = "blockrooms.13_8.saved_inventory";
    private static final String BOAT_SINK_TAG = "blockrooms.13_8.boat_sink";
    private static final int BOAT_SINK_TICKS = 1200;


    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel sl : event.getServer().getAllLevels()) {
            if (!sl.dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
                continue;
            }
            if (!sl.isRaining()) {
                sl.setWeatherParameters(0, 24000, true, false);
            }
        }
    }


    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getDimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            saveInventory(player);
            giveStarterKit(player);
        } else if (player.level().dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            restoreInventory(player);
        }
    }

    private static void saveInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                slots.add(i);
                stacks.add(s);
            }
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeInt(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            buf.writeInt(slots.get(i));
            ItemStack.STREAM_CODEC.encode(buf, stacks.get(i));
        }
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        player.getPersistentData().putByteArray(SAVED_INVENTORY_TAG, data);
    }

    private static void restoreInventory(ServerPlayer player) {
        byte[] data = player.getPersistentData().getByteArray(SAVED_INVENTORY_TAG).orElse(new byte[0]);
        Inventory inv = player.getInventory();
        inv.clearContent();
        if (data.length > 0) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess());
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                int slot = buf.readInt();
                ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                if (slot >= 0 && slot < inv.getContainerSize()) {
                    inv.setItem(slot, stack);
                }
            }
        }
        player.getPersistentData().remove(SAVED_INVENTORY_TAG);
    }

    private static void giveStarterKit(ServerPlayer player) {
        player.getInventory().clearContent();
        ItemStack[] kit = {
                new ItemStack(Items.STONE_PICKAXE),
                new ItemStack(Items.STONE_SWORD),
                new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.BOW, 2),
                new ItemStack(Items.ARROW, 64 * 2)
        };
        for (ItemStack stack : kit) {
            player.getInventory().add(stack);
        }
    }


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.level().dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            return;
        }
        if (player.isInWater()) {
            int air = player.getAirSupply();
            player.setAirSupply(Math.max(0, air - 4));
            if (air <= 0 && player.tickCount % 10 == 0) {
                player.hurtServer((ServerLevel) player.level(), player.damageSources().drown(), 1.0F);
            }
        }
        if (player.level().isRainingAt(player.blockPosition())) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
        }
        if (hasWaterBucket(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2, false, false));
        }
    }

    private static boolean hasWaterBucket(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.WATER_BUCKET)) {
                return true;
            }
        }
        return false;
    }


    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e instanceof Boat boat && boat.level().dimension().equals(ModLevels.BLOCKLEVEL_13_8) && boat.isInWater()) {
            int t = boat.getPersistentData().getIntOr(BOAT_SINK_TAG, 0) + 1;
            if (t >= BOAT_SINK_TICKS) {
                boat.remove(Entity.RemovalReason.KILLED);
                return;
            }
            boat.getPersistentData().putInt(BOAT_SINK_TAG, t);
        }
    }


    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null || !event.getEntity().level().dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            return;
        }
        if (event.getPlacedBlock().is(ModBlocks.ERROR_CRAFTING_TABLE.get())) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public static void onMobSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
        if (!event.getLevel().getLevel().dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            return;
        }
        Entity original = event.getEntity();
        if (original instanceof Zombie zombie && !(zombie instanceof BlastZombie)) {
            event.setSpawnCancelled(true);
            spawnReplacement(event, ModEntities.BLAST_ZOMBIE.get(), original);
        } else if (original instanceof Creeper creeper && !(creeper instanceof SculkCreeper)) {
            event.setSpawnCancelled(true);
            spawnReplacement(event, ModEntities.SCULK_CREEPER.get(), original);
        }
    }

    private static void spawnReplacement(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event,
                                         EntityType<? extends Mob> type, Entity original) {
        ServerLevel level = event.getLevel().getLevel();
        Mob replacement = type.create(level, EntitySpawnReason.NATURAL);
        replacement.setPos(original.getX(), original.getY() + (original instanceof Zombie ? 3 : 0), original.getZ());
        replacement.setYRot(original.getYRot());
        replacement.setXRot(original.getXRot());
        replacement.finalizeSpawn(event.getLevel(), level.getCurrentDifficultyAt(original.blockPosition()),
                EntitySpawnReason.NATURAL, null);
        level.addFreshEntity(replacement);
    }


    @SubscribeEvent
    public static void onChat(net.neoforged.neoforge.event.ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!player.level().dimension().equals(ModLevels.BLOCKLEVEL_13_8)) {
            return;
        }
        for (BlastZombie bz : player.level().getEntitiesOfClass(BlastZombie.class, player.getBoundingBox().inflate(64))) {
            bz.setTarget(player);
        }
    }
}