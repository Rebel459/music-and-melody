package net.rebel459.music_and_melody.client.util;

import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Shared visual tokens for the custom Music and Melody screens.
 *
 * <p>The values intentionally remain plain ARGB integers so renderers can use
 * them directly.  A future album can replace these values without having to
 * find colours spread across individual screens.</p>
 */
public final class ScreenConstants {

    private ScreenConstants() {
    }

    // Surfaces and overlays
    public static final int SCREEN_BACKGROUND = 0xC9070A10;
    public static final int PANEL_BACKGROUND = 0xE5151B28;
    public static final int PANEL_OUTLINE = 0xFF3B4963;
    public static final int SOURCE_CARD_BACKGROUND = 0xFF111927;
    public static final int MODAL_BACKGROUND = 0xFF151C2A;
    public static final int DIM_OVERLAY = 0x5C000000;

    // Text roles
    public static final int TEXT_SELECTED = 0xFF9ED9A0;
    public static final int TEXT_TITLE = 0xFFFFFFFF;
    public static final int TEXT_PRIMARY = 0xFFE6EBF5;
    public static final int TEXT_DESCRIPTION = 0xFF9DA9BF;
    public static final int TEXT_HEADER = 0xFF78A6FF;
    public static final int TEXT_FAVOURITE = 0xFFD7D272;
    public static final int TEXT_EXAMPLE = 0xFF555555;
    public static final int TEXT_DISABLED = 0xFF888888;
    public static final int TEXT_PENDING_DELETION = 0xFFFF8888;

    // Interactive surfaces and progress controls
    public static final int BUTTON_HIGHLIGHT = 0xCC365985;
    public static final int BUTTON_PASSIVE = 0x66303A4D;
    public static final int PANEL_HIGHLIGHT = 0xFF78A6FF;
    public static final int BAR_BACKGROUND = 0xFF334057;
    public static final int SCROLLBAR_THUMB = 0xFF627492;
    public static final int DRAG_OUTLINE = 0xFF3B4963;

    /** Optional texture for the ordinary custom button state. */
    public static final Optional<Identifier> BUTTON_TEXTURE = Optional.empty();
    /** Optional texture for hovered, focused, or selected custom buttons. */
    public static final Optional<Identifier> BUTTON_HIGHLIGHTED_TEXTURE = Optional.empty();
    /** Optional texture for inactive custom buttons. */
    public static final Optional<Identifier> BUTTON_DISABLED_TEXTURE = Optional.empty();

    /** Converts an ARGB token to the RGB value expected by text styles. */
    public static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }
}
