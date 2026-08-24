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

/**
 * BlockLevel 303「浮云一梦之城」规则：
 * <ul>
 *   <li>常驻生命恢复 III + 缓降 II（无限时长，离开即消失）；</li>
 *   <li>免疫摔落 / 弓箭 / 铁砧 / 直接玩家攻击伤害；</li>
 *   <li>无法破坏原有建筑（仅允许破坏草/土/农田/作物/石头等自然方块）；</li>
 *   <li>敌对生物无法生成；TNT 等破坏性物品立即消失；</li>
 *   <li>携带黑曜石、锁链护腿、信标会被立即清除；</li>
 *   <li>出生大楼地下层（y&lt;0）→ 传送回进入本域层之前的域层；</li>
 *   <li>跳入（西北）虚空 → BlockLevel N（暂占位 BlockLevel 0）；</li>
 *   <li>郊区作物生长约 9.6 倍、动物生长约 2 倍；</li>
 *   <li>出生大楼二楼村民首次进入时生成。</li>
 * </ul>
 */
@EventBusSubscriber
public class BlockLevel303Handler {
    private static final String PREV_LEVEL_TAG = "blockrooms.303.prev_level";
    private static final Map<UUID, ResourceKey<Level>> LAST_DIMENSION = new HashMap<>();
    private static boolean villagersSpawned = false;

    // ---------- 玩家 tick ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ResourceKey<Level> dim = level.dimension();

        // 记录进入前的域层
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

        // 常驻效果：生命恢复 III + 缓降 II（-1 = 无限，HUD 显示 ∞）
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, -1, 1, false, false));

        // 禁带物品：黑曜石 / 锁链护腿 / 信标 立即清除（每 20 tick 检查）
        if (player.tickCount % 20 == 0) {
            removeBannedItems(player);
        }

        // 出生大楼二楼村民（首次）
        if (!villagersSpawned && player.tickCount % 40 == 0) {
            spawnVillagers(level);
        }

        // 地下层：出生大楼范围内 y < 0 → 回退到进入前的域层
        if (player.getY() < 0 && BlockLevel303Generator.inSpawnBuilding(player.getBlockX(), player.getBlockZ())) {
            teleportBack(player);
            return;
        }

        // 跳入（西北）虚空 → BlockLevel N（由附属模组 blockroomsjokes 实现；
        // 未安装时占位 BlockLevel 0）
        if (player.getY() < level.getMinY() - 32) {
            TeleportUtils.teleportPlayer(player, blocklevelNTarget(level));
            return;
        }

        // 作物加速：每 20 tick 对玩家周围 3×3 区块的作物补跑 randomTick ×9（≈9.6 倍）
        if (player.tickCount % 20 == 0) {
            accelerateCrops(level, player.blockPosition());
        }
    }

    // ---------- 伤害免疫 ----------

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

    // ---------- 无法破坏建筑 ----------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            return;
        }
        if (!isBreakable(event.getState())) {
            event.setCanceled(true);
        }
    }

    /** 白名单：仅自然方块可破坏（草/土/农田/作物/石头等），建筑不可破坏 */
    private static boolean isBreakable(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.FARMLAND
                || b == Blocks.STONE || b == Blocks.GRAVEL || b == Blocks.SAND
                || b instanceof CropBlock;
    }

    // ---------- 敌对生物拦截 ----------

    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof Monster
                && event.getLevel().getLevel().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            event.setSpawnCancelled(true);
        }
    }

    // ---------- 破坏性物品消失 ----------

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

    // ---------- 动物生长加速 ----------

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AgeableMob mob && mob.isBaby()
                && mob.level().dimension().equals(ModLevels.BLOCKLEVEL_303)) {
            mob.setAge(mob.getAge() + 1); // 等效 2 倍生长
        }
    }

    // ---------- 工具 ----------

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

    /**
     * BlockLevel N 目标：附属模组 blockroomsjokes 已加载且其 blockleveln 维度存在时前往，
     * 否则占位 BlockLevel 0（BLN 由附属模组实现，主模组不包含）。
     */
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

    /** 出生大楼二楼村民（懒生成，每服务器一次；二楼地板 y=6） */
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

    /** 作物加速：玩家周围 3×3 区块的作物补跑 randomTick ×9 */
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
