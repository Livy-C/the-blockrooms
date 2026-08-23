package name.blockrooms.client.hud;

import name.blockrooms.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.ArrayList;
import java.util.List;
public final class DifficultyLayer implements GuiLayer {
    private static final int SCREEN_MARGIN = 4;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int BACKGROUND_ALPHA = 0x70;

    private static final DifficultyLayer INSTANCE = new DifficultyLayer();

    public static DifficultyLayer instance() {
        return INSTANCE;
    }

    private DifficultyLayer() {
    }

    private LevelInfoData.Difficulty difficulty;
    public void show(LevelInfoData.Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void hide() {
        this.difficulty = null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (difficulty == null || !ClientConfig.LEVEL_DIFFICULTY_ENABLED.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) {
            return;
        }

        List<String> texts = new ArrayList<>(4);
        List<Integer> colors = new ArrayList<>(4);
        if (difficulty.title() != null && !difficulty.title().isBlank()) {
            texts.add(difficulty.title());
            colors.add(difficulty.titleColor());
        }
        if (difficulty.safe() != null) {
            texts.add(difficulty.safe());
            colors.add(difficulty.safeColor());
        }
        if (difficulty.security() != null) {
            texts.add(difficulty.security());
            colors.add(difficulty.securityColor());
        }
        if (difficulty.entity() != null) {
            texts.add(difficulty.entity());
            colors.add(difficulty.entityColor());
        }
        if (texts.isEmpty()) {
            return;
        }

        Font font = mc.font;
        int lineHeight = font.lineHeight + 1;
        int panelWidth = PADDING_X * 2;
        for (String text : texts) {
            panelWidth = Math.max(panelWidth, font.width(text) + PADDING_X * 2);
        }
        int panelHeight = texts.size() * lineHeight + PADDING_Y * 2;
        int x = mc.getWindow().getGuiScaledWidth() - SCREEN_MARGIN - panelWidth;
        int y = SCREEN_MARGIN;

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_ALPHA << 24);

        int textY = y + PADDING_Y;
        for (int i = 0; i < texts.size(); i++) {
            guiGraphics.drawString(font, Component.literal(texts.get(i)), x + PADDING_X, textY,
                    colors.get(i) | 0xFF000000, true);
            textY += lineHeight;
        }
    }
}
