package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.rebel459.music_and_melody.client.Theme;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThemeHelper {

    private ThemeHelper() {
    }

    public static int BACKGROUND;
    public static int PANEL_BACKGROUND;
    public static int PANEL_OUTLINE;
    public static int POPUP_OUTLINE;
    public static int POPUP_PANEL_BACKGROUND;
    public static int POPUP_OVERLAY;

    public static int TEXT_SELECTED;
    public static int TEXT_TITLE;
    public static int TEXT_PRIMARY;
    public static int TEXT_PRIMARY_HIGHLIGHT;
    public static int TEXT_DESCRIPTION;
    public static int TEXT_HEADER;
    public static int TEXT_HEADER_SECONDARY;
    public static int TEXT_FAVOURITE;
    public static int TEXT_EXAMPLE;
    public static int TEXT_DISABLED;
    public static int TEXT_PENDING_DELETION;
    public static boolean TEXT_SHADOW;

    private static Identifier appliedTheme;

    public static int BUTTON_HIGHLIGHT;
    public static int BUTTON_PASSIVE;
    public static int BUTTON_DISABLED;
    public static boolean BUTTON_TEXTURES;
    public static int PANEL_HIGHLIGHT;
    public static int BAR_BACKGROUND;
    public static int SCROLLBAR_THUMB;
    public static int DRAG_OUTLINE;

    public static void apply(Theme theme) {
        if (theme == null) return;

        if (!theme.valid) return;
        appliedTheme = theme.theme;
        BUTTON_SCALING.clear();
        BACKGROUND = argb(theme.panels.background());
        PANEL_BACKGROUND = argb(theme.panels.panelBackground());
        PANEL_OUTLINE = argb(theme.panels.panelOutline());
        PANEL_HIGHLIGHT = argb(theme.panels.panelHighlight());
        POPUP_OUTLINE = argb(theme.panels.popupOutline());
        POPUP_PANEL_BACKGROUND = argb(theme.panels.popupPanelBackground());
        POPUP_OVERLAY = argb(theme.panels.popupOverlay());

        BUTTON_PASSIVE = argb(theme.elements.buttonBackground());
        BUTTON_HIGHLIGHT = argb(theme.elements.buttonHighlight());
        BUTTON_DISABLED = argb(theme.elements.buttonDisabled());
        BUTTON_TEXTURES = theme.elements.buttonTextures();
        DRAG_OUTLINE = argb(theme.elements.outline());
        BAR_BACKGROUND = argb(theme.elements.barBackground());
        SCROLLBAR_THUMB = argb(theme.elements.barThumb());

        TEXT_SELECTED = argb(theme.text.selected());
        TEXT_TITLE = argb(theme.text.title());
        TEXT_PRIMARY = argb(theme.text.primary());
        TEXT_PRIMARY_HIGHLIGHT = argb(theme.text.primaryHighlight());
        TEXT_DESCRIPTION = argb(theme.text.description());
        TEXT_HEADER = argb(theme.text.header());
        TEXT_HEADER_SECONDARY = argb(theme.text.headerSecondary());
        TEXT_FAVOURITE = argb(theme.text.favourite());
        TEXT_EXAMPLE = argb(theme.text.example());
        TEXT_DISABLED = argb(theme.text.disabled());
        TEXT_PENDING_DELETION = argb(theme.text.warning());
        TEXT_SHADOW = theme.text.shadow();
    }

    private static int argb(String value) {
        if (value == null) return 0;
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 8) return 0;
        try {
            return (int) Long.parseLong(normalized, 16);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int colour) {
        graphics.text(font, text, x, y, colour, TEXT_SHADOW);
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int colour) {
        graphics.text(font, text, x, y, colour, TEXT_SHADOW);
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int colour) {
        graphics.text(font, text, x, y, colour, TEXT_SHADOW);
    }

    public static void centeredText(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int colour) {
        text(graphics, font, text, x - font.width(text) / 2, y, colour);
    }
    public static void centeredText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int colour) {
        text(graphics, font, text, x - font.width(text) / 2, y, colour);
    }

    public static final Identifier VANILLA_BUTTON = Identifier.withDefaultNamespace("widget/button");
    public static final Identifier VANILLA_BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    public static final Identifier VANILLA_BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

    public static final List<Identifier> THEME_OVERRIDES = List.of(
            IconButton.icon("always_enabled"),
            IconButton.icon("back"),
            IconButton.icon("built_in"),
            IconButton.icon("clear"),
            IconButton.icon("config"),
            IconButton.icon("delete"),
            IconButton.icon("disabled"),
            IconButton.icon("download"),
            IconButton.icon("downloading"),
            IconButton.icon("enabled"),
            IconButton.icon("favourite"),
            IconButton.icon("locked"),
            IconButton.icon("loop"),
            IconButton.icon("looping"),
            IconButton.icon("manage"),
            IconButton.icon("next"),
            IconButton.icon("pause"),
            IconButton.icon("play"),
            IconButton.icon("previous"),
            IconButton.icon("queue"),
            IconButton.icon("reload"),
            IconButton.icon("remove"),
            IconButton.icon("restore"),
            IconButton.icon("retry_download"),
            IconButton.icon("save"),
            IconButton.icon("search"),
            IconButton.icon("shuffle_off"),
            IconButton.icon("shuffle_on"),
            IconButton.icon("unlocked"),
            IconButton.icon("update"),
            VANILLA_BUTTON,
            VANILLA_BUTTON_HIGHLIGHTED,
            VANILLA_BUTTON_DISABLED
    );

    private static final Map<Identifier, GuiSpriteScaling> BUTTON_SCALING = new HashMap<>();

    public static Identifier getThemeTexture(Identifier icon) {
        if (icon == null || !THEME_OVERRIDES.contains(icon)) return icon;

        Identifier theme = currentTheme();
        if (theme == null) return icon;

        String name = fileName(icon);
        String path = "textures/theme/" + theme.getPath() + "/" + name;
        String resourcePath = path.endsWith(".png") ? path : path + ".png";
        Identifier texture = Identifier.fromNamespaceAndPath(theme.getNamespace(), resourcePath);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) return texture;
        return icon;
    }

    // Renders a themed button texture without requiring the GUI atlas
    public static boolean renderThemeButton(GuiGraphicsExtractor graphics, Identifier button, int x, int y, int width, int height, float alpha) {
        Identifier texture = getThemeTexture(button);
        if (texture.equals(button)) return false;

        GuiSpriteScaling scaling = BUTTON_SCALING.computeIfAbsent(texture, ThemeHelper::readButtonScaling);
        int color = ARGB.white(alpha);
        if (!(scaling instanceof GuiSpriteScaling.NineSlice nineSlice)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                    width, height, width, height, color);
            return true;
        }

        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
        int left = Math.min(border.left(), width / 2);
        int right = Math.min(border.right(), width / 2);
        int top = Math.min(border.top(), height / 2);
        int bottom = Math.min(border.bottom(), height / 2);
        int[] sourceX = {0, left, nineSlice.width() - right};
        int[] sourceY = {0, top, nineSlice.height() - bottom};
        int[] sourceWidth = {left, nineSlice.width() - left - right, right};
        int[] sourceHeight = {top, nineSlice.height() - top - bottom, bottom};
        int[] targetX = {x, x + left, x + width - right};
        int[] targetY = {y, y + top, y + height - bottom};
        int[] targetWidth = {left, width - left - right, right};
        int[] targetHeight = {top, height - top - bottom, bottom};

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (sourceWidth[column] <= 0 || sourceHeight[row] <= 0
                        || targetWidth[column] <= 0 || targetHeight[row] <= 0) continue;
                if (nineSlice.stretchInner() || (row % 2 == 0 && column % 2 == 0)) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                            targetX[column], targetY[row], sourceX[column], sourceY[row],
                            sourceWidth[column], sourceHeight[row], targetWidth[column], targetHeight[row],
                            nineSlice.width(), nineSlice.height(), color);
                    continue;
                }
                for (int tileY = 0; tileY < targetHeight[row]; tileY += sourceHeight[row]) {
                    int tileHeight = Math.min(sourceHeight[row], targetHeight[row] - tileY);
                    for (int tileX = 0; tileX < targetWidth[column]; tileX += sourceWidth[column]) {
                        int tileWidth = Math.min(sourceWidth[column], targetWidth[column] - tileX);
                        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                                targetX[column] + tileX, targetY[row] + tileY,
                                sourceX[column], sourceY[row], tileWidth, tileHeight,
                                tileWidth, tileHeight, nineSlice.width(), nineSlice.height(), color);
                    }
                }
            }
        }
        return true;
    }

    private static GuiSpriteScaling readButtonScaling(Identifier texture) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
            if (resource == null) return GuiSpriteScaling.DEFAULT;
            return resource.metadata().getSection(GuiMetadataSection.TYPE)
                    .map(GuiMetadataSection::scaling)
                    .orElse(GuiSpriteScaling.DEFAULT);
        } catch (IOException ignored) {
            return GuiSpriteScaling.DEFAULT;
        }
    }

    private static Identifier currentTheme() {
        if (appliedTheme != null) return appliedTheme;
        return Identifier.tryParse(MaMDataConfig.get().active_theme);
    }

    private static String fileName(Identifier identifier) {
        String path = identifier.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }

}
