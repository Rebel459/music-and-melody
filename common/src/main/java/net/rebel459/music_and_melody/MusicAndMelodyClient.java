package net.rebel459.music_and_melody;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.unified.api.client.core.UnifiedClientEvents;
import net.rebel459.unified.api.client.core.UnifiedClientHelpers;
import net.rebel459.unified.api.client.core.UnifiedClientRegistries;
import net.rebel459.unified.api.core.UnifiedHelpers;
import net.rebel459.unified.api.event.EventTiming;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public final class MusicAndMelodyClient {

    public static UnifiedClientRegistries.KeyMappings KEY_MAPPINGS = UnifiedClientRegistries.KeyMappings.create(MusicAndMelody.MOD_ID);

    public static final Supplier<KeyMapping> PLAYLIST_KEY = KEY_MAPPINGS.registerKeybind(
            "playlist",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.Category.MISC
    );

    public static void initRegistries() {
        if (MaMClientConfig.get().music_rebalance) {
            UnifiedClientHelpers.RESOURCE_PACKS.addRequired(MusicAndMelody.id("music_and_melody"));
        }
    }

    public static void init() {
        UnifiedClientEvents.Instance.onTick(EventTiming.POST, client -> {
            while (PLAYLIST_KEY.get().consumeClick()) {
                client.gui.setScreen(new PlaylistScreen(client.gui.screen()));
            }
        });
    }
}
