package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, Blockrooms.MODID);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL0_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel0_generator", () -> BlockLevel0Generator.CODEC);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL4_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel4_generator", () -> BlockLevel4Generator.CODEC);
    public static void register(IEventBus eventBus) { CHUNK_GENERATORS.register(eventBus); }
}
