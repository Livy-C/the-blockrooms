package name.blockrooms.entity.secret;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModSecretEntities {
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Blockrooms.MODID);
    private static ResourceKey<EntityType<?>> entityId(Identifier id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }

    public static final DeferredHolder<EntityType<?>, EntityType<BlockroomWill>> BLOCKROOM_WILL =
            ENTITY_TYPES.register("blockroom_will", id ->
                    EntityType.Builder.of(BlockroomWill::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.8F)
                            .clientTrackingRange(24)
                            .fireImmune()
                            .build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<ChainEntity>> CHAIN =
            ENTITY_TYPES.register("chain", id ->
                    EntityType.Builder.of(ChainEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.0F)
                            .clientTrackingRange(16)
                            .build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<Puppet>> PUPPET =
            ENTITY_TYPES.register("puppet", id ->
                    EntityType.Builder.of(Puppet::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(entityId(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<PlayerPuppet>> PLAYER_PUPPET =
            ENTITY_TYPES.register("player_puppet", id ->
                    EntityType.Builder.of((EntityType<PlayerPuppet> type, Level level) -> new PlayerPuppet(type, level), MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(entityId(id)));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
