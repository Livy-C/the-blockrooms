package name.blockrooms.entity;

import name.blockrooms.Blockrooms;
import name.blockrooms.entity.projectiles.BlockProjectile;
import name.blockrooms.entity.projectiles.ItemProjectile;
import name.blockrooms.entity.secret.SecretEntityOne;
import net.minecraft.core.registries.Registries;import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Blockrooms.MODID);
    private static ResourceKey<EntityType<?>> entityId(Identifier id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<ItemProjectile>> ITEM_PROJECTILE =
            ENTITY_TYPES.register("item_projectile",id ->
                    EntityType.Builder.of(ItemProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<BlockProjectile>> BLOCK_PROJECTILE =
            ENTITY_TYPES.register("block_projectile", id ->
                    EntityType.Builder.of(BlockProjectile::new, MobCategory.MISC).sized(1.0f,1.0f).build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<BloodZombie>> BLOOD_ZOMBIE =
            ENTITY_TYPES.register("blood_zombie", id ->
                    EntityType.Builder.of(BloodZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .build(entityId(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<EnhancedSkeleton>> SKELETON = ENTITY_TYPES.register("skeleton", id -> EntityType.Builder.of(EnhancedSkeleton::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .build(entityId(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackstoneShulker>> BLACKSTONE_SHULKER =
            ENTITY_TYPES.register("blackstone_shulker", id ->
                    EntityType.Builder.of(BlackstoneShulker::new, MobCategory.MONSTER)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(10)
                            .build(entityId(id)));

    /** 秘密实体一号（块室意志的投影，占位）：恼鬼模型巨大化 + 锁链装饰 */
    public static final DeferredHolder<EntityType<?>, EntityType<SecretEntityOne>> SECRET_ENTITY_ONE =
            ENTITY_TYPES.register("secret_entity_one", id ->
                    EntityType.Builder.of(SecretEntityOne::new, MobCategory.MISC)
                            .sized(0.4F, 0.8F)
                            .clientTrackingRange(16)
                            .build(entityId(id)));

    /** 爆破僵尸（BL13.8）：会飞的 TNT 僵尸 */
    public static final DeferredHolder<EntityType<?>, EntityType<BlastZombie>> BLAST_ZOMBIE =
            ENTITY_TYPES.register("blast_zombie", id ->
                    EntityType.Builder.of(BlastZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(entityId(id)));

    /** 幽匿苦力怕（BL13.8）：4 倍血量 + 隔墙爆炸 */
    public static final DeferredHolder<EntityType<?>, EntityType<SculkCreeper>> SCULK_CREEPER =
            ENTITY_TYPES.register("sculk_creeper", id ->
                    EntityType.Builder.of(SculkCreeper::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(10)
                            .build(entityId(id)));


    public static void register(IEventBus bus){
        ENTITY_TYPES.register(bus);
    }
}
