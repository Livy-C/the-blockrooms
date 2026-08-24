package name.blockrooms.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import name.blockrooms.util.ModLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotMixin {

    @ModifyVariable(
            method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"),
            index = 2)
    private static Consumer<NativeImage> blockrooms$glitchScreenshotInSite404(Consumer<NativeImage> original) {
        if (!isInSite404()) {
            return original;
        }
        return image -> {
            glitchImage(image);
            original.accept(image);
        };
    }

    private static boolean isInSite404() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(ModLevels.SITE_404);
    }

    /** 故障化：扫描线错位 + 随机色块 + 通道轮换 */
    private static void glitchImage(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        RandomSource random = RandomSource.create();

        // 1. 扫描线错位：随机水平条带整体平移
        for (int b = 0; b < 24; b++) {
            int y = random.nextInt(h);
            int bandH = 1 + random.nextInt(6);
            int shift = random.nextInt(-40, 41);
            for (int yy = y; yy < Math.min(h, y + bandH); yy++) {
                int[] row = new int[w];
                for (int x = 0; x < w; x++) {
                    row[x] = image.getPixel(x, yy);
                }
                for (int x = 0; x < w; x++) {
                    image.setPixel(x, yy, row[Math.floorMod(x - shift, w)]);
                }
            }
        }

        // 2. 随机色块
        for (int r = 0; r < 16; r++) {
            int rx = random.nextInt(w);
            int ry = random.nextInt(h);
            int rw = 4 + random.nextInt(48);
            int rh = 2 + random.nextInt(24);
            int color = 0xFF000000 | random.nextInt(0xFFFFFF);
            for (int yy = ry; yy < Math.min(h, ry + rh); yy++) {
                for (int xx = rx; xx < Math.min(w, rx + rw); xx++) {
                    image.setPixel(xx, yy, color);
                }
            }
        }

        // 3. 通道错位：每隔几行 RGB 轮换
        for (int yy = 0; yy < h; yy += 2 + random.nextInt(5)) {
            for (int x = 0; x < w; x++) {
                int c = image.getPixel(x, yy);
                int r = ARGB.red(c);
                int g = ARGB.green(c);
                int b = ARGB.blue(c);
                image.setPixel(x, yy, ARGB.color(255, b, r, g));
            }
        }
    }
}
