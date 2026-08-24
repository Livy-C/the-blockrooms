package name.blockrooms.util;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class ModLevels {
    private static final Set<ResourceKey<Level>> BLOCKLEVELS = new HashSet<>();
    //public static final TagKey<Level> IN_BLOCKROOMS = TagKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "in_blockrooms"));

    public static final ResourceKey<Level> BLOCKLEVEL_0 = level("blocklevel0");
    public static final ResourceKey<Level> BLOCKLEVEL_1 = level("blocklevel1");
    public static final ResourceKey<Level> BLOCKLEVEL_2 = level("blocklevel2");
    public static final ResourceKey<Level> BLOCKLEVEL_3 = level("blocklevel3");
    public static final ResourceKey<Level> BLOCKLEVEL_4 = level("blocklevel4");
    /** 空值之室（Level Null）：虚空中的石头平台 */
    public static final ResourceKey<Level> BLOCKLEVEL_NULL = level("blocklevel_null");
    /** BlockLevel 15：凝灰岩平原 + 拱门 */
    public static final ResourceKey<Level> BLOCKLEVEL_15 = level("blocklevel15");
    /** BlockLevel 303：浮云一梦之城（无尽混凝土城市 + 郊区） */
    public static final ResourceKey<Level> BLOCKLEVEL_303 = level("blocklevel303");
    /** BlockLevel 13.8：往迹浸复湮，来径遂芜废（海洋 + 暴雨 + 危险水域） */
    public static final ResourceKey<Level> BLOCKLEVEL_13_8 = level("blocklevel13_8");
    /** Site 404：ERROR! 404 Not Found（纯黑虚空 + 故障） */
    public static final ResourceKey<Level> SITE_404 = level("site_404");
    public static final ResourceKey<Level> GALLERY = level("the_gallery");

    public static boolean isInBlockrooms(ResourceKey<Level> key){
        return BLOCKLEVELS.stream().anyMatch(key::equals);
    }

    private static ResourceKey<Level> level(String key) {
        var a = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, key));
        BLOCKLEVELS.add(a);
        return a;
    }
}
