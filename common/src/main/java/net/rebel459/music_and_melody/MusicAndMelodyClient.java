package net.rebel459.music_and_melody;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.client.ThemeListener;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.client.util.CombatStatus;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.unified.api.client.core.UnifiedClientEvents;
import net.rebel459.unified.api.client.core.UnifiedClientHelpers;
import net.rebel459.unified.api.event.EventTiming;
import org.lwjgl.glfw.GLFW;

public final class MusicAndMelodyClient {

    public static final KeyMapping PLAYLIST_KEY = new KeyMapping(
            "key." + MusicAndMelody.MOD_ID + ".music_player",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.Category.MISC
    );

    public static void initRegistries() {
        UnifiedClientHelpers.KEY_MAPPINGS.register(PLAYLIST_KEY);
        if (MaMClientConfig.get().music_rebalance) {
            UnifiedClientHelpers.RESOURCE_PACKS.addRequired(MusicAndMelody.id("music_and_melody"));
        }
        UnifiedClientHelpers.RELOAD_LISTENERS.addListener(AlbumListener.ID, new AlbumListener());
        UnifiedClientHelpers.RELOAD_LISTENERS.addListener(PlaylistListener.ID, new PlaylistListener());
        UnifiedClientHelpers.RELOAD_LISTENERS.addListener(EventListener.ID, new EventListener());
        UnifiedClientHelpers.RELOAD_LISTENERS.addListener(ThemeListener.ID, new ThemeListener());
    }

    public static void init() {
        CombatStatus.init();
        UnifiedClientEvents.Instance.onTick(EventTiming.POST, client -> {
            while (PLAYLIST_KEY.consumeClick()) {
                client.setScreen(new MusicPlayerScreen(client.screen));
            }
        });
    }
}
