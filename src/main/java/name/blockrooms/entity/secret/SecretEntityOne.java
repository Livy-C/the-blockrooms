package name.blockrooms.entity.secret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class SecretEntityOne extends Vex {

    private static final double SCALE = 5.0;
    private static final int CHAIN_DIST = 3;
    private static final int CHAIN_LENGTH = 4;

    private boolean chainsPlaced;

    public SecretEntityOne(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        var scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(SCALE);
        }
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!chainsPlaced && !this.level().isClientSide()) {
            chainsPlaced = true;
            placeChains();
        }
    }

    private void placeChains() {
        BlockPos base = this.blockPosition();
        int[][] offsets = {{CHAIN_DIST, 0}, {-CHAIN_DIST, 0}, {0, CHAIN_DIST}, {0, -CHAIN_DIST}};
        for (int[] off : offsets) {
            int x = base.getX() + off[0];
            int z = base.getZ() + off[1];
            for (int y = Math.max(this.level().getMinY(), base.getY() - CHAIN_LENGTH); y < base.getY(); y++) {
                BlockPos p = new BlockPos(x, y, z);
                if (this.level().getBlockState(p).isAir()) {
                    this.level().setBlock(p, Blocks.IRON_CHAIN.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }
    }
}