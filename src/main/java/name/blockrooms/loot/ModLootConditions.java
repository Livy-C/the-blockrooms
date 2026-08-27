package name.blockrooms.loot;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModLootConditions {
    private static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Blockrooms.MODID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> IN_BLOCKLEVEL_13_8 =
            CONDITIONS.register("in_blocklevel_13_8", () -> new LootItemConditionType(InBlockLevel13_8Condition.CODEC));

    public static void register(IEventBus eventBus) {
        CONDITIONS.register(eventBus);
    }
}