package name.blockrooms.client.renderer;

import name.blockrooms.Blockrooms;
import name.blockrooms.entity.secret.BlockroomWill;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

/**
 * 块室意志渲染：Vex 模型（巨大恼鬼外观）+ 专属贴图。
 * 体型缩放由 Attributes.SCALE 控制（200 倍）。
 */
public class BlockroomWillRenderer extends MobRenderer<BlockroomWill, VexRenderState, VexModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "textures/entity/blockroom_will.png");

    public BlockroomWillRenderer(EntityRendererProvider.Context context) {
        super(context, new VexModel(context.bakeLayer(ModelLayers.VEX)), 0.5F);
    }

    @Override
    public VexRenderState createRenderState() {
        return new VexRenderState();
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return TEXTURE;
    }
}
