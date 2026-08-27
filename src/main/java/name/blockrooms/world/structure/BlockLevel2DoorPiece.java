package name.blockrooms.world.structure;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.entity.TeleporterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jspecify.annotations.Nullable;

public class BlockLevel2DoorPiece extends StructurePiece {
    private static final String ANCHOR_X_TAG = "AX";
    private static final String ANCHOR_Y_TAG = "AY";
    private static final String ANCHOR_Z_TAG = "AZ";
    private static final String FACING_TAG = "FACING";
    private static final String QUARTZ_TAG = "QUARTZ";
    private static final String TARGET_TAG = "TARGET";

    private static final int WALL_WIDTH = 3;
    private static final int WALL_HEIGHT = 3;
    private static final int DEPTH = 2;

    private final BlockPos anchor;
    private final Direction facing;
    private final boolean quartz;
    @Nullable
    private final ResourceKey<Level> targetDimension;

    public BlockLevel2DoorPiece(BlockPos anchor, Direction facing, boolean quartz, @Nullable ResourceKey<Level> targetDimension) {
        super(ModStructures.BL2_DOOR_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(anchor, anchor.offset(WALL_WIDTH - 1, WALL_HEIGHT - 1, DEPTH - 1)));
        this.anchor = anchor;
        this.facing = facing;
        this.quartz = quartz;
        this.targetDimension = targetDimension;
    }

    public BlockLevel2DoorPiece(CompoundTag tag) {
        super(ModStructures.BL2_DOOR_PIECE_TYPE.get(), tag);
        this.anchor = new BlockPos(tag.getIntOr(ANCHOR_X_TAG, 0), tag.getIntOr(ANCHOR_Y_TAG, 0), tag.getIntOr(ANCHOR_Z_TAG, 0));
        this.facing = Direction.from2DDataValue(tag.getIntOr(FACING_TAG, 0));
        this.quartz = tag.getIntOr(QUARTZ_TAG, 0) != 0;
        String dim = tag.getStringOr(TARGET_TAG, "");
        this.targetDimension = dim.isEmpty()
                ? null
                : ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(ANCHOR_X_TAG, anchor.getX());
        tag.putInt(ANCHOR_Y_TAG, anchor.getY());
        tag.putInt(ANCHOR_Z_TAG, anchor.getZ());
        tag.putInt(FACING_TAG, facing.get2DDataValue());
        tag.putInt(QUARTZ_TAG, quartz ? 1 : 0);
        if (targetDimension != null) {
            tag.putString(TARGET_TAG, targetDimension.identifier().toString());
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        int ax = anchor.getX();
        int ay = anchor.getY();
        int az = anchor.getZ();

        BlockPos spot = findWallSpot(level, chunkPos, ax, ay, az);
        if (spot == null) {
            return;
        }
        ax = spot.getX();
        ay = spot.getY();
        az = spot.getZ();

        BlockState frame = quartz ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();

        for (int dx = 0; dx <= WALL_WIDTH - 1; dx++) {
            for (int dy = 0; dy <= WALL_HEIGHT - 1; dy++) {
                for (int dz = 0; dz <= DEPTH - 1; dz++) {
                    BlockState state;
                    if (dz == 0) {
                        if (dx == 1 && dy <= 1) {
                            if (!quartz && dy == 0) {
                                state = doorBlock(DoubleBlockHalf.LOWER);
                            } else if (!quartz && dy == 1) {
                                state = doorBlock(DoubleBlockHalf.UPPER);
                            } else {
                                state = Blocks.CAVE_AIR.defaultBlockState();
                            }
                        } else {
                            state = frame;
                        }
                    } else {
                        if (dx == 1 && dy == 0 || dx == 1 && dy == 1) {
                            state = ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState();

                        } else {
                            state = frame;
                        }
                    }
                    setBlock(level, box, ax + dx, ay + dy, az + dz, state);
                }
            }
        }

        if (targetDimension != null) {
            BlockPos tp = new BlockPos(ax + 1, ay, az + 1);
            if (box.isInside(tp) || box.isInside(tp.above())) {
                TeleporterBlockEntity blockEntity = new TeleporterBlockEntity(tp, level.getBlockState(tp));
                TeleporterBlockEntity blockEntity1 = new TeleporterBlockEntity(tp.above(), level.getBlockState(tp.above()));
                blockEntity.getTargets().add(new TeleporterBlockEntity.Target(targetDimension, null));
                blockEntity1.getTargets().add(new TeleporterBlockEntity.Target(targetDimension, null));
                level.getChunk(tp).setBlockEntity(blockEntity);
                level.getChunk(tp.above()).setBlockEntity(blockEntity);
            }
        }
    }

    private @Nullable BlockPos findWallSpot(WorldGenLevel level, ChunkPos chunkPos, int ax, int ay, int az) {
        if (fitsInWall(level, ax, ay, az)) {
            return new BlockPos(ax, ay, az);
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        for (int nz = minZ + 1; nz <= minZ + 14; nz++) {
            for (int nx = minX; nx <= minX + 13; nx++) {
                if (nx == ax && nz == az) continue;
                if (fitsInWall(level, nx, ay, nz)) {
                    return new BlockPos(nx, ay, nz);
                }
            }
        }
        return null;
    }

    private boolean fitsInWall(WorldGenLevel level, int ax, int ay, int az) {
        for (int dx = 0; dx <= WALL_WIDTH - 1; dx++) {
            for (int dy = 0; dy <= WALL_HEIGHT - 1; dy++) {
                for (int dz = 0; dz <= DEPTH - 1; dz++) {
                    if (dx == 1 && dy <= 1) continue;
                    if (dx == 1 && dz == 1 && dy <= 1) continue;
                    BlockState s = level.getBlockState(new BlockPos(ax + dx, ay + dy, az + dz));
                    if (s.isAir()) {
                        return false;
                    }
                }
            }
        }
        BlockPos front = new BlockPos(ax + 1, ay, az - 1);
        BlockPos back = new BlockPos(ax + 1, ay, az + 1);
        return level.getBlockState(front).isAir() || level.getBlockState(back).isAir();
    }

    private BlockState doorBlock(DoubleBlockHalf half) {
        return Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, half);
    }

    private static void setBlock(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }
}