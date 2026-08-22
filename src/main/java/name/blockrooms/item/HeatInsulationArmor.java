package name.blockrooms.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 隔热套装工具类：以 {@link Predicate}（判定）与 {@link Consumer}（执行）表达套装逻辑。
 * <ul>
 *   <li>{@link #HAS_FULL_SET}：是否穿满 4 件；</li>
 *   <li>{@link #APPLY_PASSIVE_EFFECT}：穿满时被动刷新火焰抗性（隔热效果）；</li>
 *   <li>{@link #wornCount}：已穿件数（供温度模块按件减免伤害）。</li>
 * </ul>
 */
public final class HeatInsulationArmor {
    /** 判定：是否穿满 4 件隔热装备 */
    public static final Predicate<LivingEntity> HAS_FULL_SET = HeatInsulationArmor::isFullSet;

    /** 执行：穿满整套时持续给予火焰抗性（隔热），未穿满则无效果 */
    public static final Consumer<LivingEntity> APPLY_PASSIVE_EFFECT = entity -> {
        if (HAS_FULL_SET.test(entity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false));
        }
    };

    public static int wornCount(LivingEntity entity) {
        int count = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HEAT_INSULATION_HELMET.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.HEAT_INSULATION_CHESTPLATE.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.HEAT_INSULATION_LEGGINGS.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.HEAT_INSULATION_BOOTS.get())) {
            count++;
        }
        return count;
    }

    public static boolean isFullSet(LivingEntity entity) {
        return wornCount(entity) == 4;
    }

    private HeatInsulationArmor() {
    }
}
