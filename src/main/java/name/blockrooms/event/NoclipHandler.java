package name.blockrooms.event;

import name.blockrooms.Blockrooms;
import name.blockrooms.util.FlexibleMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Set;

public class NoclipHandler {
    public record levelWithChance(ResourceKey<Level> level, double chance) {}
    private static final FlexibleMap<ResourceKey<Level>, BlockState, levelWithChance> noclipMap = new FlexibleMap<>();
    private static ResourceKey<Level> level(String key) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, key));
    }
    @SuppressWarnings("UnusedReturnValue")
    private static boolean teleportPlayer(ServerPlayer player, ResourceKey<Level> level, double x, double y, double z) {
        return player.teleportTo(player.level().getServer().getLevel(level), x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
    }

    public static void noclipByCondition(ServerPlayer player, BlockState state) {
        levelWithChance destination = noclipMap.get(player.level().dimension(), state);
        if (destination == null) return;
        if (!(player.getRandom().nextDouble() <= destination.chance)) return;

        teleportPlayer(player, destination.level, player.getX(), 1, player.getZ());
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!(event.getSource().is(DamageTypes.IN_WALL))) return;

        noclipByCondition(player, player.getInBlockState());
    }

    static {
        noclipMap.put(Level.OVERWORLD,
                new levelWithChance(level("blocklevel0"), 0.2));
        noclipMap.put(Level.OVERWORLD, Blocks.AMETHYST_BLOCK.defaultBlockState(),
                new levelWithChance(Level.NETHER, 0.9));
        noclipMap.put(level("blocklevel0"), Blocks.AMETHYST_BLOCK.defaultBlockState(),
                new levelWithChance(Level.END, 0.9));
    }
}