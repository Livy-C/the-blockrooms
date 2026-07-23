package name.blockrooms.client.renderer;

import name.blockrooms.entity.BlockProjectile;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockProjectileRenderState extends EntityRenderState {
    public BlockProjectile.BlockRenderState blockRenderState;

    public boolean hasSubState() {
        return this.blockRenderState != null;
    }
}

