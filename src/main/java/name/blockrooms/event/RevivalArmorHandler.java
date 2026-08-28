package name.blockrooms.event;

import name.blockrooms.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * 复苏套装（全套 4 件）效果：
 * 1. 无限生命恢复 III + 饱和；
 * 2. 生命值低于一半时获得速度 II + 力量 I；
 * 3. 穿戴全套时免疫负面效果。
 */
@EventBusSubscriber
public class RevivalArmorHandler {

    private static final Set<Holder<MobEffect>> NEGATIVE_EFFECTS = Set.of(
            MobEffects.SLOWNESS,
            MobEffects.MINING_FATIGUE,
            MobEffects.NAUSEA,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.GLOWING,
            MobEffects.LEVITATION,
            MobEffects.UNLUCK,
            MobEffects.BAD_OMEN,
            MobEffects.DARKNESS,
            MobEffects.TRIAL_OMEN,
            MobEffects.RAID_OMEN,
            MobEffects.OOZING,
            MobEffects.INFESTED
    );

    private static final Set<UUID> FULL_SET_PLAYERS = new HashSet<>();

    private static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.REVIVAL_HELMET)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.REVIVAL_CHESTPLATE)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.REVIVAL_LEGGINGS)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.REVIVAL_BOOTS);
    }

    private static void ensureEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() <= 60) {
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, false));
        }
    }
    private static void removeIfInfinite(Player player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.isInfiniteDuration()) {
            player.removeEffect(effect);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!isWearingFullSet(player)) {
            if (FULL_SET_PLAYERS.remove(player.getUUID())) {
                removeIfInfinite(player, MobEffects.REGENERATION);
                removeIfInfinite(player, MobEffects.SATURATION);
                removeIfInfinite(player, MobEffects.SPEED);
                removeIfInfinite(player, MobEffects.STRENGTH);
            }
            return;
        }

        FULL_SET_PLAYERS.add(player.getUUID());
        ensureEffect(player, MobEffects.REGENERATION, 2);   // 生命恢复 III（0 基 2）
        ensureEffect(player, MobEffects.SATURATION, 0);     // 饱和

        if (player.getHealth() < player.getMaxHealth() / 2.0F) {
            ensureEffect(player, MobEffects.SPEED, 1);        // 速度 II（0 基 1）
            ensureEffect(player, MobEffects.STRENGTH, 0);     // 力量 I
        } else {
            removeIfInfinite(player, MobEffects.SPEED);
            removeIfInfinite(player, MobEffects.STRENGTH);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!isWearingFullSet(entity)) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (isNegative(effect)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static boolean isNegative(Holder<MobEffect> effect) {
        return effect.value().getCategory().equals(MobEffectCategory.HARMFUL);
    }
}
