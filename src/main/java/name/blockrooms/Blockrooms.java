package name.blockrooms;

import com.mojang.logging.LogUtils;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.entity.ModBlockEntities;
import name.blockrooms.block.recipe.ModRecipeTypes;
import name.blockrooms.compat.TanTemperatureCompat;
import name.blockrooms.effect.ModMobEffects;
import name.blockrooms.entity.BloodZombie;
import name.blockrooms.entity.EnhancedSkeleton;
import name.blockrooms.entity.ModEntities;
import name.blockrooms.entity.secret.BlockroomWill;
import name.blockrooms.entity.secret.ChainEntity;
import name.blockrooms.entity.secret.Puppet;
import name.blockrooms.item.ModCreativeModeTabs;
import name.blockrooms.item.ModItems;
import name.blockrooms.item.components.ModDataComponents;
import name.blockrooms.loot.ModLootConditions;
import name.blockrooms.network.TemperaturePayload;
import name.blockrooms.sounds.ModSounds;
import name.blockrooms.world.generator.ModGenerators;
import name.blockrooms.world.structure.ModStructures;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(Blockrooms.MODID)
public class Blockrooms {
    public static final String MODID = "blockrooms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Blockrooms(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPlacements);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::addPackFinders);
        modEventBus.addListener(this::registerPayloads);

        ModMobEffects.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModGenerators.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEntities.register(modEventBus);
        name.blockrooms.entity.secret.ModSecretEntities.register(modEventBus);
        ModStructures.register(modEventBus);
        ModLootConditions.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("toughasnails")) {
            TanTemperatureCompat.register();
        }
    }

    private void registerPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.BLOOD_ZOMBIE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BLOOD_ZOMBIE.get(), BloodZombie.createAttributes().build());
        event.put(ModEntities.SKELETON.get(), EnhancedSkeleton.createAttributes().build());
        event.put(ModEntities.BLACKSTONE_SHULKER.get(), Shulker.createAttributes().build());
        event.put(ModEntities.BLAST_ZOMBIE.get(), net.minecraft.world.entity.monster.zombie.Zombie.createAttributes().build());
        event.put(ModEntities.SCULK_CREEPER.get(), net.minecraft.world.entity.monster.Creeper.createAttributes().build());
        event.put(name.blockrooms.entity.secret.ModSecretEntities.BLOCKROOM_WILL.get(), BlockroomWill.createAttributes().build());
        event.put(name.blockrooms.entity.secret.ModSecretEntities.CHAIN.get(), ChainEntity.createAttributes().build());
        event.put(name.blockrooms.entity.secret.ModSecretEntities.PUPPET.get(), Puppet.createAttributes().build());
        event.put(name.blockrooms.entity.secret.ModSecretEntities.PLAYER_PUPPET.get(),
                net.minecraft.world.entity.player.Player.createAttributes().build());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipeTypes.ERROR_CRAFTING.get());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Blockrooms.MODID);
        registrar.playToClient(TemperaturePayload.TYPE, TemperaturePayload.STREAM_CODEC, TemperaturePayload::handle);
    }


    private void addPackFinders(AddPackFindersEvent event) {
//        if (event.getPackType() == PackType.SERVER_DATA) {
//            event.addPackFinders(
//                    Identifier.fromNamespaceAndPath(Blockrooms.MODID, "recipe_tweaks"),
//                    PackType.SERVER_DATA,
//                    Component.literal("The Blockrooms Recipe Tweaks"),
//                    PackSource.BUILT_IN,
//                    true,
//                    Pack.Position.TOP);
//        }
    }
}
