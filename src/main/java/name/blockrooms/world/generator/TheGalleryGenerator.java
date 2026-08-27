package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

public class TheGalleryGenerator extends BaseBlockLevelGenerator {

    public static final int CORRIDOR_SPACING = 24;

    public static final int INNER_Z_MIN = 1;
    public static final int INNER_Z_MAX = 4;

    public static final int WALL_REL_Z_NEG = 0;
    public static final int WALL_REL_Z_POS = 5;

    public static final int FLOOR_Y = 0;
    public static final int CEILING_Y = 5;
    public static final int BEDROCK_Y = 6;

    public static final int SPAWN_Z = 2;

    public static final int LINK_INTERVAL = 24;
    public static final int PASSAGE_X_OFFSET = 6;

    private static final int PAINT_ANCHOR_Y = 2;

    private static final int LAMP_INTERVAL = 8;
    private static final int LAMP_REL_Z = 2;

    private static final double DECOR_CHANCE = 0.5;
    private static final double EXIT_FRAME_CHANCE = 0.04;
    private static final int[] DECOR_X = {2, 3, 4, 5, 10, 11, 12};

    public static final MapCodec<TheGalleryGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(TheGalleryGenerator::getBiomeSource)
            ).apply(instance, TheGalleryGenerator::new)
    );

    public TheGalleryGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            int worldX = minBlockX + x;
            boolean lampColumn = Math.floorMod(worldX, LAMP_INTERVAL) == 0;
            for (int lz = 0; lz < 16; lz++) {
                int wz = minBlockZ + lz;
                int relZ = Math.floorMod(wz, CORRIDOR_SPACING);
                boolean interior = relZ >= INNER_Z_MIN && relZ <= INNER_Z_MAX;
                for (int y = this.getMinY(); y <= BEDROCK_Y; y++) {
                    BlockState state;
                    if (y == BEDROCK_Y) {
                        state = Blocks.BEDROCK.defaultBlockState();
                    } else if (interior) {
                        if (y == FLOOR_Y || y == CEILING_Y) {
                            state = Blocks.OAK_PLANKS.defaultBlockState();
                        } else if (lampColumn && relZ == LAMP_REL_Z && y == CEILING_Y - 1) {
                            state = Blocks.IRON_CHAIN.defaultBlockState();
                        } else if (lampColumn && relZ == LAMP_REL_Z && y == CEILING_Y - 2) {
                            state = Blocks.LANTERN.defaultBlockState();
                        } else {
                            state = Blocks.CAVE_AIR.defaultBlockState();
                        }
                    } else {
                        state = Blocks.OAK_PLANKS.defaultBlockState();
                    }
                    chunk.setBlockState(new BlockPos(x, y, lz), state, Block.UPDATE_NONE);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        ChunkPos chunkPos = worldGenRegion.getCenter();

        long seed = worldGenRegion.getSeed();
        Random random = new Random(seed ^ (chunkPos.x * 0x9e3779b97f4a7c15L) ^ (chunkPos.z * 0xdefacedddeedbeefL));

        List<Holder<PaintingVariant>> largeVariants = variants(worldGenRegion, 4, 4);
        List<Holder<PaintingVariant>> passageVariants = variants(worldGenRegion, 4, 3);
        if (passageVariants.isEmpty()) return;

        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();

        for (int lz = 0; lz < 16; lz++) {
            int wz = minBlockZ + lz;
            int relZ = Math.floorMod(wz, CORRIDOR_SPACING);
            final boolean westWall;
            if (relZ == WALL_REL_Z_NEG) {
                westWall = true;
            } else if (relZ == WALL_REL_Z_POS) {
                westWall = false;
            } else {
                continue;
            }

            int k = Math.floorDiv(wz, CORRIDOR_SPACING);
            int wallWorldZ = k * CORRIDOR_SPACING + relZ;
            int anchorZ = westWall ? wallWorldZ + 1 : wallWorldZ - 1;
            Direction facing = westWall ? Direction.SOUTH : Direction.NORTH;

            for (int lx = 0; lx < 16; lx++) {
                int wx = minBlockX + lx;
                if (Math.floorMod(wx, LINK_INTERVAL) == PASSAGE_X_OFFSET) {
                    placePainting(worldGenRegion, passageVariants,
                            new BlockPos(wx, PAINT_ANCHOR_Y, anchorZ), facing);
                }
            }

            if (random.nextDouble() < DECOR_CHANCE) {
                int decorLx = DECOR_X[random.nextInt(DECOR_X.length)];
                int decorWx = minBlockX + decorLx;
                if (Math.floorMod(decorWx, LINK_INTERVAL) != PASSAGE_X_OFFSET) {
                    BlockPos anchor = new BlockPos(decorWx, PAINT_ANCHOR_Y, anchorZ);
                    if (random.nextDouble() < EXIT_FRAME_CHANCE) {
                        ItemFrame frame = new ItemFrame(worldGenRegion.getLevel(), anchor, facing);
                        worldGenRegion.addFreshEntity(frame);
                        worldGenRegion.getLevel().getServer().execute(() -> {
                            if (frame.isAlive()) {
                                frame.setItem(new ItemStack(Items.PAINTING));
                            }
                        });
                        worldGenRegion.getChunk(anchor.below()).setBlockState(anchor.below(),
                                Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing),
                                Block.UPDATE_NONE);
                    } else {
                        placePainting(worldGenRegion, largeVariants, anchor, facing);
                    }
                }
            }
        }
    }

    private static List<Holder<PaintingVariant>> variants(WorldGenRegion region, int width, int height) {
        return StreamSupport.stream(region.registryAccess()
                        .lookupOrThrow(Registries.PAINTING_VARIANT)
                        .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                        .spliterator(), false)
                .filter(variant -> variant.value().width() == width && variant.value().height() == height)
                .toList();
    }

    private static void placePainting(WorldGenRegion region,
                                      List<Holder<PaintingVariant>> variants, BlockPos anchor, Direction facing) {
        Optional<Holder<PaintingVariant>> variant = Util.getRandomSafe(variants, region.getRandom());
        if (variant.isEmpty()) return;
        region.addFreshEntity(new Painting(region.getLevel(), anchor, facing, variant.get()));
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }
}