package name.blockrooms.event.level;

import name.blockrooms.item.ModItems;
import name.blockrooms.util.ModLevels;
import name.blockrooms.world.generator.BlockLevelNullGenerator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class NullLevelHandler {
    private static final String ENTER_TAG = "blockrooms.null.enter_time";
    private static final long HAZARD_AFTER_TICKS = 12000L;
    private static final long HAZARD_INTERVAL = 800L;
    private static final double BEAM_RADIUS = 1.0;
    private static final int BEAM_HEIGHT = 48;
    private static final int BEAM_EFFECT_DURATION = 100;


    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        for (ServerLevel sl : event.getServer().getAllLevels()) {
            if (!sl.dimension().equals(ModLevels.BLOCKLEVEL_NULL)) {
                continue;
            }
            long time = sl.getGameTime();
            if (time % 5 != 0) {
                continue;
            }
            int baseY = BlockLevelNullGenerator.PLATFORM_Y + 1;
            for (int i = 0; i < 3; i++) {
                double py = baseY + ((double) time / 5 + i * 16) % BEAM_HEIGHT;
                sl.sendParticles(new DustParticleOptions(0xFF8C8C8C, 1.0F),
                        0.0, py, 0.0, 1, 0.3, 0.0, 0.3, 0.0);
            }
            if (time % 40 == 0 && sl.getBlockEntity(BlockLevelNullGenerator.BARREL_POS) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
                int count = 0;
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    var stack = barrel.getItem(i);
                    if (stack.is(ModItems.ALMOND_MILK_BUCKET.get())) {
                        count += stack.getCount();
                    }
                }
                if (count < 9) {
                    barrel.setItem(0, new net.minecraft.world.item.ItemStack(ModItems.ALMOND_MILK_BUCKET.get(), 1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.level();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_NULL)) {
            return;
        }
        long time = level.getGameTime();

        var data = player.getPersistentData();
        long entered = data.getLongOr(ENTER_TAG, 0L);
        if (entered == 0L) {
            data.putLong(ENTER_TAG, time);
            if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
            }
            entered = time;
        }

        double dx = player.getX();
        double dz = player.getZ();
        if (Math.abs(dx) <= BEAM_RADIUS && Math.abs(dz) <= BEAM_RADIUS && player.getY() > BlockLevelNullGenerator.PLATFORM_Y) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, BEAM_EFFECT_DURATION, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, BEAM_EFFECT_DURATION, 0, false, true));
        }
        if (time - entered >= HAZARD_AFTER_TICKS) {
            if ((time - entered) % HAZARD_INTERVAL < 20) {
                int duration = 30 * 20;
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 1, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
            }
        }
    }


    @SubscribeEvent
    public static void onDrinkAlmondMilk(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getItem().is(ModItems.ALMOND_MILK_BUCKET.get())) {
            return;
        }
        ServerLevel level = player.level();
        if (!level.dimension().equals(ModLevels.BLOCKLEVEL_NULL)) {
            return;
        }
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.getPersistentData().putLong(ENTER_TAG, level.getGameTime());
    }
}