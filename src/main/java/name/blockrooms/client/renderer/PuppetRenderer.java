package name.blockrooms.client.renderer;

import name.blockrooms.Blockrooms;
import name.blockrooms.entity.secret.Puppet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * 普通傀儡渲染：人形模型 + 傀儡贴图。
 * 继承 HumanoidMobRenderer 以正确填充四肢/手持姿态。
 */
public class PuppetRenderer extends HumanoidMobRenderer<Puppet, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "textures/entity/puppet.png");

    public PuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
