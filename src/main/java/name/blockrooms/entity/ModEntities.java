package name.blockrooms.entity;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Blockrooms.MODID);
    private static ResourceKey<EntityType<?>> entityId(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(name));
    }
    private static ResourceKey<EntityType<?>> entityId(Identifier id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<ItemProjectile>> ITEM_PROJECTILE =
            ENTITY_TYPES.register("item_projectile",id ->
                    EntityType.Builder.of(ItemProjectile::new, MobCategory.MISC).build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<BlockProjectile>> BLOCK_PROJECTILE =
            ENTITY_TYPES.register("block_projectile", id ->
                    EntityType.Builder.of(BlockProjectile::new, MobCategory.MISC).build(entityId(id)));


    public static void register(IEventBus bus){
        ENTITY_TYPES.register(bus);
    }
}
