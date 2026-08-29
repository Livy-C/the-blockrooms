package name.blockrooms.client.renderer;

import name.blockrooms.entity.secret.PlayerPuppet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * 玩家傀儡渲染：人形模型 + 默认 Steve 皮肤（占位）。
 * Player 不是 Mob，无法继承 HumanoidMobRenderer，手动调用 extractHumanoidRenderState 填充姿态。
 */
public class PlayerPuppetRenderer extends LivingEntityRenderer<PlayerPuppet, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;

    public PlayerPuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(PlayerPuppet entity, HumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
