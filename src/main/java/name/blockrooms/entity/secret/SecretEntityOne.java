package name.blockrooms.entity.secret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 秘密实体一号：块室意志的投影（占位版）。
 * <p>
 * 暂用原版纹理：恼鬼模型 + {@link Attributes#SCALE} 巨大化（5 倍）+ 无 AI 静止悬浮，
 * 首次 tick 时在四周垂下原版铁链（{@link Blocks#CHAIN}）作为"被锁链束缚"的占位装饰。
 * 后续将接入：随机域层投影生成、精神污染、尖啸、傀儡化等逻辑。
 */
public class SecretEntityOne extends Vex {

    /** 巨大化倍率（占位） */
    private static final double SCALE = 5.0;
    /** 四周锁链柱的偏移与长度 */
    private static final int CHAIN_DIST = 3;
    private static final int CHAIN_LENGTH = 4;

    private boolean chainsPlaced;

    public SecretEntityOne(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        var scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(SCALE);
        }
        this.setNoAi(true);   // 静止悬浮，不主动活动
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

    /** 占位装饰：四周垂下 4 根铁链柱 */
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
