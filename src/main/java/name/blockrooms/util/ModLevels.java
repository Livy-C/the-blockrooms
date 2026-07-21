package name.blockrooms.util;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ModLevels {
    public static final ResourceKey<Level> BLOCKLEVEL_0 = level("blocklevel0");
    public static final ResourceKey<Level> BLOCKLEVEL_4 = level("blocklevel4");
    private static ResourceKey<Level> level(String key) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, key));
    }
}
