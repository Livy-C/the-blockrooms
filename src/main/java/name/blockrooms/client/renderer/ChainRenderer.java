package name.blockrooms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import name.blockrooms.entity.secret.ChainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 锁链渲染：用原版铁链方块（chain block）沿 Y 轴堆叠并放大。
 * 视觉：底部垂直，越往上越向目标点（Boss 身体）弯曲收拢，
 * 形成"锁链从地面斜拉束缚住 Boss"的感觉。
 */
public class ChainRenderer extends EntityRenderer<ChainEntity, ChainRenderer.ChainRenderState> {

    /** 锁链放大倍数 */
    private static final float SCALE = 8.0F;
    /** 渲染段数上限（性能保护） */
    private static final int MAX_SEGMENTS = 24;

    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;

    public ChainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    public static class ChainRenderState extends EntityRenderState {
        public int chainLength = 1;
        public boolean visible = true;
        /** 底部锚点（锁链实体自身位置） */
        public double baseX, baseY, baseZ;
        /** 顶部目标点（Boss 身体钩住位置） */
        public double targetX, targetY, targetZ;
    }

    @Override
    public ChainRenderState createRenderState() {
        return new ChainRenderState();
    }

    @Override
    public void extractRenderState(ChainEntity entity, ChainRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.chainLength = entity.getChainLength();
        state.visible = entity.isAlive();
        state.baseX = entity.getX();
        state.baseY = entity.getY();
        state.baseZ = entity.getZ();
        state.targetX = entity.getTargetX();
        state.targetY = entity.getTargetY();
        state.targetZ = entity.getTargetZ();
    }

    @Override
    public void submit(ChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (!state.visible || state.chainLength <= 0) return;

        BlockState chainState = Blocks.IRON_CHAIN.defaultBlockState();
        BlockStateModel model = this.blockRenderer.getBlockModel(chainState);

        int segments = Math.min(Math.max(1, state.chainLength), MAX_SEGMENTS);
        // 目标点相对底部的偏移（世界坐标 → 实体本地坐标）
        double dx = state.targetX - state.baseX;
        double dy = state.targetY - state.baseY;
        double dz = state.targetZ - state.baseZ;

        for (int i = 0; i < segments; i++) {
            // t: 0=底部 → 1=顶部
            float t = segments <= 1 ? 0 : (float) i / (segments - 1);
            // 二次曲线：底部垂直（水平偏移小），顶部快速收拢向目标点
            float curve = t * t;
            poseStack.pushPose();
            poseStack.translate(
                    (float) (dx * curve),
                    t * (float) dy,
                    (float) (dz * curve));
            // 放大铁链方块（以段中心为锚）
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.translate(0.0F, 0.5F, 0.0F);
            if (i % 2 == 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            collector.submitBlockModel(
                    poseStack,
                    RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
                    model,
                    1.0F, 1.0F, 1.0F,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0);
            poseStack.popPose();
        }
    }
}
