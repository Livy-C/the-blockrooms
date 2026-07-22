package name.blockrooms.client.renderer;

import name.blockrooms.entity.BlockProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BlockProjectileRenderer extends EntityRenderer<BlockProjectile, BlockProjectileRenderState> {
    protected BlockProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BlockProjectileRenderState createRenderState() {
        return new BlockProjectileRenderState();
    }
}
