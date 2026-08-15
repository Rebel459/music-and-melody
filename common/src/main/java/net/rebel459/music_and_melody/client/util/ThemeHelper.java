package net.rebel459.music_and_melody.client.util;

import net.rebel459.music_and_melody.client.Theme;

/**
 * Shared visual tokens for the custom Music and Melody screens.
 *
 * <p>The values intentionally remain plain ARGB integers so renderers can use
 * them directly.  A future album can replace these values without having to
 * find colours spread across individual screens.</p>
 */
public final class ThemeHelper {

    private ThemeHelper() {
    }

    // Surfaces and overlays
    public static int SCREEN_BACKGROUND;
    public static int PANEL_BACKGROUND;
    public static int PANEL_OUTLINE;
    public static int POPUP_OUTLINE;
    public static int MODAL_BACKGROUND;
    public static int DIM_OVERLAY;

    // Text roles
    public static int TEXT_SELECTED;
    public static int TEXT_TITLE;
    public static int TEXT_PRIMARY;
    public static int TEXT_DESCRIPTION;
    public static int TEXT_HEADER;
    public static int TEXT_FAVOURITE;
    public static int TEXT_EXAMPLE;
    public static int TEXT_DISABLED;
    public static int TEXT_PENDING_DELETION;

    // Interactive surfaces and progress controls
    public static int BUTTON_HIGHLIGHT;
    public static int BUTTON_PASSIVE;
    public static boolean BUTTON_TEXTURES;
    public static int PANEL_HIGHLIGHT;
    public static int BAR_BACKGROUND;
    public static int SCROLLBAR_THUMB;
    public static int DRAG_OUTLINE;

    /** Applies a fully resolved theme to the tokens used by the custom UI. */
    public static void apply(Theme theme) {
        if (theme == null) return;

        if (!theme.valid) return;
        SCREEN_BACKGROUND = argb(theme.panels.background());
        PANEL_BACKGROUND = argb(theme.panels.panelBackground());
        PANEL_OUTLINE = argb(theme.panels.panelOutline());
        PANEL_HIGHLIGHT = argb(theme.panels.panelHighlight());
        POPUP_OUTLINE = argb(theme.panels.popupOutline());
        MODAL_BACKGROUND = argb(theme.panels.popupPanelBackground());
        DIM_OVERLAY = argb(theme.panels.popupOverlay());

        BUTTON_PASSIVE = argb(theme.elements.buttonBackground());
        BUTTON_HIGHLIGHT = argb(theme.elements.buttonHighlight());
        BUTTON_TEXTURES = theme.elements.buttonTextures();
        DRAG_OUTLINE = argb(theme.elements.outline());
        BAR_BACKGROUND = argb(theme.elements.barBackground());
        SCROLLBAR_THUMB = argb(theme.elements.barThumb());

        TEXT_SELECTED = argb(theme.text.selected());
        TEXT_TITLE = argb(theme.text.title());
        TEXT_PRIMARY = argb(theme.text.primary());
        TEXT_DESCRIPTION = argb(theme.text.description());
        TEXT_HEADER = argb(theme.text.header());
        TEXT_FAVOURITE = argb(theme.text.favourite());
        TEXT_EXAMPLE = argb(theme.text.example());
        TEXT_DISABLED = argb(theme.text.disabled());
        TEXT_PENDING_DELETION = argb(theme.text.warning());
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

    /** Converts an ARGB token to the RGB value expected by text styles. */
    public static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }

}
