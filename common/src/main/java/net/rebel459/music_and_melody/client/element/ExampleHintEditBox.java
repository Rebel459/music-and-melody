package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

/**
 * Single-line edit box whose hint uses the same opacity as vanilla's
 * multi-line placeholder text.
 */
public class ExampleHintEditBox extends EditBox {

    private static final int MULTI_LINE_PLACEHOLDER_ALPHA = 0xCC;
    private int normalTextColor = EditBox.DEFAULT_TEXT_COLOR;

    public ExampleHintEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    public void setNormalTextColor(int color) {
        this.normalTextColor = color;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int textColor = this.getValue().isEmpty() && !this.isFocused()
                ? ARGB.color(MULTI_LINE_PLACEHOLDER_ALPHA, ThemeHelper.rgb(ThemeHelper.TEXT_EXAMPLE))
                : this.normalTextColor;
        this.setTextColor(textColor);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, tickDelta);
    }
}
