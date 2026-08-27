package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.concurrent.CompletableFuture;

public class BlockLevel3Generator extends BaseBlockLevelGenerator {

    public static final int LAYER_HEIGHT = 5;
    public static final int TUNNEL_MIN = 5;
    public static final int TUNNEL_MAX = 9;
    public static final int TRAP_MIN_Y = 60;

    private static final Identifier[] DRIVE_TEMPLATES = {
            id("bl3_drive1"), id("bl3_drive2"), id("bl3_drive2_notramcar"), id("bl3_drive3"),
            id("bl3_drive3_box"), id("bl3_drive4_button"), id("bl3_drive4_button2"),
            id("bl3_drive_dark1"), id("bl3_drive_dark2"), id("bl3_drive_dark2_notramcar")
    };
    private static final Identifier[] ACROSS_TEMPLATES = {
            id("bl3_across_drive1"), id("bl3_across_drive2"), id("bl3_across_drive2_notramcar"),
            id("bl3_across_drive2_notramcar2"), id("bl3_across_drive3_button"),
            id("bl3_across_drive3_button2"), id("bl3_across_drive_dark"), id("bl3_across_drive_dark2")
    };
    private static final Identifier JUNCTION = id("bl3_junction");
    private static final Identifier[] SPECIAL_X = {id("bl3_fake_exit"), id("bl3_music")};
    private static final Identifier SPECIAL_Z = id("bl3_award");
    private static final Identifier TRAP = id("bl3_trap");

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, name);
    }

    public static final MapCodec<BlockLevel3Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel3Generator::getBiomeSource)
            ).apply(instance, BlockLevel3Generator::new)
    );

    public BlockLevel3Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {

        int minY = this.getMinY();
        int maxY = minY + this.getGenDepth();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();
        int maxLayer = (this.getGenDepth() - 1) / LAYER_HEIGHT;
        for (int layer = 0; layer <= maxLayer; layer++) {
            int baseY = layer * LAYER_HEIGHT;
            generateLayer(level, chunk, cp, seed, layer, baseY);
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
    }


    private void generateLayer(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed, int layer, int baseY) {
        boolean east = edgeOpen(seed, cp.x, cp.z, layer, Direction.EAST);
        boolean west = edgeOpen(seed, cp.x, cp.z, layer, Direction.WEST);
        boolean south = edgeOpen(seed, cp.x, cp.z, layer, Direction.SOUTH);
        boolean north = edgeOpen(seed, cp.x, cp.z, layer, Direction.NORTH);
        boolean xTunnel = east || west;
        boolean zTunnel = south || north;

        if (xTunnel) {
            for (int x = 0; x < 16; x++) {
                for (int z = TUNNEL_MIN; z <= TUNNEL_MAX; z++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
            if (!west) {
                for (int z = TUNNEL_MIN; z <= TUNNEL_MAX; z++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(0, baseY + y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
            if (!east) {
                for (int z = TUNNEL_MIN; z <= TUNNEL_MAX; z++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(15, baseY + y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
        }
        if (zTunnel) {
            for (int z = 0; z < 16; z++) {
                for (int x = TUNNEL_MIN; x <= TUNNEL_MAX; x++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    }
                    if (hash(seed, cp, layer, x * 16 + z, 0x22) % 100 < 35) {
                        chunk.setBlockState(new BlockPos(x, baseY + 1, z), Blocks.REDSTONE_WIRE.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
                if (z % 4 == 0) {
                    chunk.setBlockState(new BlockPos(7, baseY + 3, z), Blocks.TORCH.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
            if (!north) {
                for (int x = TUNNEL_MIN; x <= TUNNEL_MAX; x++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
            if (!south) {
                for (int x = TUNNEL_MIN; x <= TUNNEL_MAX; x++) {
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, 15), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
        }

        placeOres(chunk, cp, seed, layer, baseY, xTunnel, zTunnel);

        if (hash(seed, cp, layer, 0, 0x33) % 28 == 0 && (xTunnel || zTunnel)) {
            int mx, mz;
            if (xTunnel) {
                mx = 2 + (int) (hash(seed, cp, layer, 1, 0x44) % 12);
                mz = 7;
            } else {
                mx = 7;
                mz = 2 + (int) (hash(seed, cp, layer, 2, 0x44) % 12);
            }
            BlockPos railPos = new BlockPos(cp.getMinBlockX() + mx, baseY + 1, cp.getMinBlockZ() + mz);
            level.setBlock(railPos, Blocks.RAIL.defaultBlockState(), Block.UPDATE_NONE);
            MinecartChest cart = new MinecartChest(EntityType.CHEST_MINECART, level.getLevel());
            cart.setPos(railPos.getX() + 0.5, railPos.getY(), railPos.getZ() + 0.5);
            level.addFreshEntity(cart);
        }
        placeLayerTemplates(level, chunk, cp, seed, layer, baseY, xTunnel, zTunnel);
        if (hash(seed, cp, layer, 3, 0x55) % 100 < 30 && (xTunnel || zTunnel)) {
            int sx = 2 + (int) (hash(seed, cp, layer, 4, 0x66) % 12);
            int sz = 7;
            if (!xTunnel) {
                sx = 7;
                sz = 2 + (int) (hash(seed, cp, layer, 5, 0x66) % 12);
            }
            for (int y = baseY + 1; y <= baseY + 5; y++) {
                chunk.setBlockState(new BlockPos(sx, y, sz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
            for (int y = baseY + 1; y <= baseY + 4; y++) {
                chunk.setBlockState(new BlockPos(sx, y, sz - 1),
                        Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH),
                        Block.UPDATE_NONE);
            }
        }
    }
    private void placeOres(ChunkAccess chunk, ChunkPos cp, long seed, int layer, int baseY, boolean xTunnel, boolean zTunnel) {
        BlockState[] ores = {
                Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.REDSTONE_ORE.defaultBlockState(),
                Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.COAL_ORE.defaultBlockState(),
                Blocks.COAL_ORE.defaultBlockState(), Blocks.LAPIS_ORE.defaultBlockState(),
                Blocks.IRON_ORE.defaultBlockState(), Blocks.EMERALD_ORE.defaultBlockState()
        };
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (hash(seed, cp, layer, x * 16 + z, 0x77) % 100 >= 10) continue;
                boolean xWall = xTunnel && (z == TUNNEL_MIN - 1 || z == TUNNEL_MAX + 1);
                boolean zWall = zTunnel && (x == TUNNEL_MIN - 1 || x == TUNNEL_MAX + 1);
                if (!xWall && !zWall) continue;
                int y = 1 + (int) (hash(seed, cp, layer, x * 16 + z, 0x88) % 3);
                BlockState ore = ores[(int) (hash(seed, cp, layer, x * 16 + z, 0x99) % ores.length)];
                chunk.setBlockState(new BlockPos(x, baseY + y, z), ore, Block.UPDATE_NONE);
            }
        }
    }


    private void placeLayerTemplates(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed,
                                     int layer, int baseY, boolean xTunnel, boolean zTunnel) {
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();

        if (xTunnel && zTunnel && hash(seed, cp, layer, 2, 0xAA) % 3 == 0) {
            placeTemplate(manager, level, JUNCTION, new BlockPos(minX + TUNNEL_MIN, baseY, minZ + TUNNEL_MIN));
        }

        if (xTunnel && hash(seed, cp, layer, 3, 0xBB) % 100 < 40) {
            Identifier tpl = DRIVE_TEMPLATES[(int) (hash(seed, cp, layer, 4, 0xCC) % DRIVE_TEMPLATES.length)];
            placeTemplate(manager, level, tpl, new BlockPos(minX, baseY, minZ + TUNNEL_MIN));
        }

        if (zTunnel && hash(seed, cp, layer, 5, 0xDD) % 100 < 40) {
            Identifier tpl = ACROSS_TEMPLATES[(int) (hash(seed, cp, layer, 6, 0xEE) % ACROSS_TEMPLATES.length)];
            placeTemplate(manager, level, tpl, new BlockPos(minX + TUNNEL_MIN, baseY, minZ));
        }
        if (hash(seed, cp, layer, 7, 0x11) % 30 == 0) {
            int pick = (int) (hash(seed, cp, layer, 8, 0x22) % 4);
            if (pick == 0 && xTunnel) {
                placeTemplate(manager, level, SPECIAL_X[0], new BlockPos(minX, baseY, minZ + TUNNEL_MIN));
            } else if (pick == 1 && zTunnel) {
                placeTemplate(manager, level, SPECIAL_Z, new BlockPos(minX + TUNNEL_MIN, baseY, minZ + 3));
            } else if (pick == 2 && xTunnel) {
                placeTemplate(manager, level, SPECIAL_X[1], new BlockPos(minX + 3, baseY, minZ + TUNNEL_MIN));
            } else if (pick == 3 && baseY >= TRAP_MIN_Y) {
                placeTemplate(manager, level, TRAP, new BlockPos(minX + 4, baseY, minZ + TUNNEL_MIN));
            }
        }
    }

    private static void placeTemplate(StructureTemplateManager manager, WorldGenLevel level,
                                      Identifier id, BlockPos origin) {
        StructureTemplate template = manager.get(id).orElse(null);
        if (template == null) {
            Blockrooms.LOGGER.warn("BL3-TPL: template {} missing", id);
            return;
        }
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_NONE);
    }

    private static boolean edgeOpen(long seed, int chunkX, int chunkZ, int layer, Direction dir) {
        int ex = chunkX;
        int ez = chunkZ;
        if (dir == Direction.WEST) {
            ex = chunkX - 1;
            dir = Direction.EAST;
        } else if (dir == Direction.NORTH) {
            ez = chunkZ - 1;
            dir = Direction.SOUTH;
        }
        long h = seed ^ (ex * 0x9E3779B97F4A7C15L) ^ (ez * 0xC2B2AE3D27D4EB4FL)
                ^ (layer * 0x165667B19E3779F9L) ^ (dir.get2DDataValue() * 0x94D049BB133111EBL);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 32)) & 0xFFFF) / 65536.0 < 0.65;
    }

    private static long hash(long seed, ChunkPos cp, int layer, int a, long salt) {
        long h = seed ^ (cp.x * 0x9E3779B97F4A7C15L) ^ (cp.z * 0xBF58476D1CE4E5B9L)
                ^ (layer * 0x165667B19E3779F9L) ^ (a * 0x94D049BB133111EBL) ^ salt;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}