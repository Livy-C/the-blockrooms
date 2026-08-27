package name.blockrooms.loot;

import com.mojang.serialization.MapCodec;
import name.blockrooms.util.ModLevels;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

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