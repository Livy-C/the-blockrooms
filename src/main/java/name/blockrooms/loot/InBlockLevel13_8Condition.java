package name.blockrooms.loot;

import com.mojang.serialization.MapCodec;
import name.blockrooms.util.ModLevels;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * 战利品条件：LootContext 所在维度是否为 BlockLevel 13.8。
 * 用于 13.8 的遗迹奖励箱战利品替换（其他维度保持原版内容）。
 */
public record InBlockLevel13_8Condition() implements LootItemCondition {
    public static final MapCodec<InBlockLevel13_8Condition> CODEC = MapCodec.unit(InBlockLevel13_8Condition::new);

    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.IN_BLOCKLEVEL_13_8.get();
    }

    @Override
    public boolean test(LootContext context) {
        return context.getLevel().dimension().equals(ModLevels.BLOCKLEVEL_13_8);
    }
}
