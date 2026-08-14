package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.screens.Screen;

/**
 * Compatibility entry point retained for integrations and existing callers.
 * The former standalone playlist screen is now the persistent player shell.
 */
public class PlaylistScreen extends MusicPlayerScreen {

    public PlaylistScreen(Screen parent) {
        super(parent, Page.NOW_PLAYING);
    }

    public PlaylistScreen(Screen parent, Page page) {
        super(parent, page);
    }
}
