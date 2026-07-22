package name.blockrooms.client.renderer;

import name.blockrooms.entity.ItemProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ItemProjectileRenderer extends EntityRenderer<ItemProjectile, ItemProjectileRenderState> {

    protected ItemProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ItemProjectileRenderState createRenderState() {
        return new ItemProjectileRenderState();
    }

}
