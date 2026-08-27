package name.blockrooms.event.level;

import name.blockrooms.Blockrooms;
import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import name.blockrooms.world.generator.BlockLevel303Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class BlockLevel303Handler {
    private static final String PREV_LEVEL_TAG = "blockrooms.303.prev_level";
    private static final Map<UUID, ResourceKey<Level>> LAST_DIMENSION = new HashMap<>();
    private static boolean villagersSpawned = false;


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ResourceKey<Level> dim = level.dimension();

        ResourceKey<Level> prev = LAST_DIMENSION.get(player.getUUID());
        if (dim.equals(ModLevels.BLOCKLEVEL_303)) {
            if (!player.getPersistentData().contains(PREV_LEVEL_TAG) && prev != null && !prev.equals(ModLevels.BLOCKLEVEL_303)) {
                player.getPersistentData().putString(PREV_LEVEL_TAG, prev.identifier().toString());
            }
        } else {
            player.getPersistentData().remove(PREV_LEVEL_TAG);
        }
        LAST_DIMENSION.put(player.getUUID(), dim);

        if (!dim.equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, -1, 1, false, false));

        if (player.tickCount % 20 == 0) {
            removeBannedItems(player);
        }

        if (!villagersSpawned && player.tickCount % 40 == 0) {
            spawnVillagers(level);
        }

        if (player.getY() < 0 && BlockLevel303Generator.inSpawnBuilding(player.getBlockX(), player.getBlockZ())) {
            teleportBack(player);
            return;
        }

        if (player.getY() < level.getMinY() - 32) {
            TeleportUtils.teleportPlayer(player, blocklevelNTarget(level));
            return;
        }

        if (player.tickCount % 20 == 0) {
            accelerateCrops(level, player.blockPosition());
        }
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }
        if (event.getSource().is(DamageTypes.FALL)
                || event.getSource().is(DamageTypes.ARROW)
                || event.getSource().is(DamageTypes.FALLING_ANVIL)
                || event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }
        if (!isBreakable(event.getState())) {
            event.setCanceled(true);
        }
    }

    private static boolean isBreakable(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.FARMLAND
                || b == Blocks.STONE || b == Blocks.GRAVEL || b == Blocks.SAND
                || b instanceof CropBlock;
    }


    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof Monster
                && event.getLevel().getLevel().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            event.setSpawnCancelled(true);
        }
    }


    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null || !event.getEntity().level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }
        if (event.getPlacedBlock().is(Blocks.TNT)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player p) {
                p.getMainHandItem().shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItem(UseItemOnBlockEvent event) {
        Player p = event.getPlayer();
        if (p == null || !p.level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }
        if (event.getItemStack().is(Items.FLINT_AND_STEEL)) {
            event.setCanceled(true);
            event.getItemStack().shrink(1);
        }
    }


    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AgeableMob mob && mob.isBaby()
                && mob.level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            mob.setAge(mob.getAge() + 1);
        }
    }


    private static void removeBannedItems(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isBanned(stack)) {
                stack.setCount(0);
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isBanned(stack)) {
                stack.setCount(0);
            }
        }
    }

    private static boolean isBanned(ItemStack stack) {
        return stack.is(Items.OBSIDIAN) || stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.BEACON);
    }

    private static ResourceKey<Level> blocklevelNTarget(ServerLevel current) {
        if (net.neoforged.fml.ModList.get().isLoaded("blockroomsjokes")) {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("blockroomsjokes", "blockleveln"));
            if (current.getServer().getLevel(key) != null) {
                return key;
            }
        }
        return ModLevels.BLOCKLEVEL_0;
    }

    private static void teleportBack(ServerPlayer player) {
        String prev = player.getPersistentData().getStringOr(PREV_LEVEL_TAG, "");
        ResourceKey<Level> target = ModLevels.BLOCKLEVEL_0;
        if (!prev.isEmpty()) {
            Identifier id = Identifier.tryParse(prev);
            if (id != null) {
                ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
                if (player.level().getServer().getLevel(key) != null) {
                    target = key;
                }
            }
        }
        TeleportUtils.teleportPlayer(player, target);
    }

    private static void spawnVillagers(ServerLevel level) {
        for (BlockPos pos : new BlockPos[]{new BlockPos(12, 6, 20), new BlockPos(20, 6, 12)}) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
            if (villager != null) {
                villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                villager.setPersistenceRequired();
                level.addFreshEntity(villager);
            }
        }
        villagersSpawned = true;
        Blockrooms.LOGGER.info("BL303: spawn building villagers spawned");
    }

    private static void accelerateCrops(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        RandomSource random = level.getRandom();
        for (int x = center.getX() - 24; x <= center.getX() + 24; x++) {
            for (int z = center.getZ() - 24; z <= center.getZ() + 24; z++) {
                for (int y = 1; y <= 3; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock) {
                        for (int i = 0; i < 9; i++) {
                            state.randomTick(level, pos, random);
                        }
                    }
                }
            }
        }
    }
}