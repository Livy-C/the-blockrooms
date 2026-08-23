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

public final class LevelInfoLayer implements GuiLayer {
    private static final int FADE_TICKS = 12;
    private static final int SCREEN_MARGIN = 4;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int BACKGROUND_ALPHA = 0x70;

    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 3;
    private static final int SCROLLBAR_TRACK_ALPHA = 0x40;
    private static final int SCROLLBAR_THUMB_ALPHA = 0xB0;

    private static final LevelInfoLayer INSTANCE = new LevelInfoLayer();

    public static LevelInfoLayer instance() {
        return INSTANCE;
    }

    private LevelInfoLayer() {
    }

    private enum Phase { TYPING, WAITING, HOLDING, FADING }
    private record Row(String text, int color, boolean pauseBefore) {
    }

    private LevelInfoData info;
    private List<Row> rows = new ArrayList<>();
    private boolean active;
    private int tickCount;
    private int rowIndex;
    private int lineStartTick;
    private Phase phase = Phase.TYPING;
    private int phaseStartTick;
    private int offset;


    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;


    public void show(LevelInfoData data) {
        this.info = data;
        this.rows = wrapRows(data);
        this.active = true;
        this.tickCount = 0;
        this.rowIndex = 0;
        this.lineStartTick = 0;
        this.phase = Phase.TYPING;
        this.phaseStartTick = 0;
        this.offset = 0;
        followScroll();
    }

    public void hide() {
        this.active = false;
        this.info = null;
        this.rows = List.of();
    }

    public boolean isActive() {
        return active;
    }

    /** True if the given scaled GUI coordinates are over the panel. */
    public boolean contains(double mouseX, double mouseY) {
        return active && mouseX >= lastX && mouseX <= lastX + lastW
                && mouseY >= lastY && mouseY <= lastY + lastH;
    }

    /** Scrolls by the given number of rows (positive = towards later content). */
    public void scrollBy(int deltaRows) {
        if (!active) {
            return;
        }
        offset = clamp(offset + deltaRows, 0, maxOffset());
    }

    public void tick() {
        if (!active) {
            return;
        }
        if (!ClientConfig.LEVEL_INFO_ENABLED.get()) {
            hide();
            return;
        }
        tickCount++;
        switch (phase) {
            case TYPING -> {
                if (typedChars() >= rows.get(rowIndex).text().length()) {
                    if (rowIndex + 1 < rows.size()) {
                        rowIndex++;
                        lineStartTick = tickCount;
                        if (rows.get(rowIndex).pauseBefore()) {
                            phase = Phase.WAITING;
                            phaseStartTick = tickCount;
                        }
                        followScroll();
                    } else {
                        phase = Phase.HOLDING;
                        phaseStartTick = tickCount;
                    }
                }
            }
            case WAITING -> {
                if (tickCount - phaseStartTick >= info.lineDelay()) {
                    phase = Phase.TYPING;
                }
            }
            case HOLDING -> {
                if (tickCount - phaseStartTick >= info.holdTicks()) {
                    phase = Phase.FADING;
                    phaseStartTick = tickCount;
                }
            }
            case FADING -> {
                if (tickCount - phaseStartTick >= FADE_TICKS) {
                    active = false;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!active || info == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        if (rows.isEmpty()) {
            return;
        }
        int alpha = alpha();
        if (alpha <= 0) {
            active = false;
            return;
        }

        boolean scrollable = rows.size() > maxRows();
        int visibleRows = scrollable ? maxRows() : rows.size();
        int contentWidth = 0;
        for (Row r : rows) {
            contentWidth = Math.max(contentWidth, font.width(r.text()));
        }
        int width = Math.min(maxWidth(),
                contentWidth + PADDING_X * 2 + (scrollable ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0));
        int lineHeight = font.lineHeight + 1;
        int height = visibleRows * lineHeight + PADDING_Y * 2;
        int x = mc.getWindow().getGuiScaledWidth() - SCREEN_MARGIN - width;
        int y = mc.getWindow().getGuiScaledHeight() - SCREEN_MARGIN - height;
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;

        guiGraphics.fill(x, y, x + width, y + height, (BACKGROUND_ALPHA * alpha / 255) << 24);

        int textX = x + PADDING_X;
        int textY = y + PADDING_Y;
        int visible = Math.min(visibleRows, rows.size() - offset);
        int drawn = Math.clamp(visible, 0, rowIndex + 1 - offset);
        for (int i = 0; i < drawn; i++) {
            int row = offset + i;
            Row r = rows.get(row);
            String text = r.text();
            if (row == rowIndex && phase == Phase.TYPING) {
                text = text.substring(0, typedChars());
            }
            if (!text.isEmpty()) {
                guiGraphics.drawString(font, Component.literal(text), textX, textY,
                        (alpha << 24) | (r.color() & 0xFFFFFF), true);
            }
            textY += lineHeight;
        }

        if (scrollable) {
            int trackX = x + width - SCROLLBAR_WIDTH - 1;
            int trackY = y + 1;
            int trackH = height - 2;
            guiGraphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackH,
                    (SCROLLBAR_TRACK_ALPHA * alpha / 255) << 24);
            int thumbH = Math.max(8, trackH * visibleRows / rows.size());
            int thumbY = trackY + (trackH - thumbH) * offset / Math.max(1, maxOffset());
            guiGraphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH,
                    (SCROLLBAR_THUMB_ALPHA * alpha / 255) << 24);
        }
    }

    private int maxWidth() {
        return ClientConfig.LEVEL_INFO_PANEL_WIDTH.get();
    }

    private int maxRows() {
        return ClientConfig.LEVEL_INFO_PANEL_ROWS.get();
    }

    private int maxOffset() {
        return Math.max(0, rows.size() - maxRows());
    }

    private void followScroll() {
        if (rows.isEmpty()) {
            return;
        }
        int visibleRows = Math.min(rows.size(), maxRows());
        if (rowIndex < offset) {
            offset = rowIndex;
        }
        if (rowIndex >= offset + visibleRows) {
            offset = clamp(rowIndex - visibleRows + 1, 0, maxOffset());
        }
    }

    private List<Row> wrapRows(LevelInfoData data) {
        Font font = Minecraft.getInstance().font;
        int wrapWidth = Math.max(10, maxWidth() - PADDING_X * 2 - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
        List<Row> result = new ArrayList<>();
        boolean firstRow = true;
        if (data.hasTitle()) {
            List<String> pieces = wrap(font, data.title(), wrapWidth);
            for (String piece : pieces) {
                result.add(new Row(piece, data.titleColor(), false));
                firstRow = false;
            }
        }
        for (LevelInfoData.Line line : data.lines()) {
            List<String> pieces = wrap(font, line.text(), wrapWidth);
            for (int i = 0; i < pieces.size(); i++) {
                result.add(new Row(pieces.get(i), line.color(), !firstRow && i == 0));
                firstRow = false;
            }
        }
        return result;
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text.isEmpty()) {
            result.add("");
            return result;
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            if (!line.isEmpty() && font.width(line + ch) > maxWidth) {
                result.add(line.toString());
                line.setLength(0);
            }
            line.append(ch);
            i += Character.charCount(cp);
        }
        result.add(line.toString());
        return result;
    }

    private String currentText() {
        return rows.get(rowIndex).text();
    }

    private int typedChars() {
        String text = currentText();
        if (phase != Phase.TYPING) {
            return text.length();
        }
        return Math.min(text.length(), 1 + (tickCount - lineStartTick) / Math.max(1, info.typeSpeed()));
    }

    private int alpha() {
        if (phase != Phase.FADING) {
            return 255;
        }
        return Math.max(0, 255 - 255 * (tickCount - phaseStartTick) / FADE_TICKS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }
}
