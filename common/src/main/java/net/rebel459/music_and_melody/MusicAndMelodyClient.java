package net.rebel459.music_and_melody;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.platform.client.UnifiedClientEvents;
import net.rebel459.unified.platform.client.UnifiedClientRegistries;
import net.rebel459.unified.util.EventType;
import net.rebel459.unified.util.PackType;
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
            UnifiedHelpers.PACKS.add(MusicAndMelody.id("music_and_melody"), PackType.REQUIRED_RESOURCES);
        }
    }

    public static void init() {
        UnifiedClientEvents.Instance.onTick(EventType.POST, client -> {
            while (PLAYLIST_KEY.get().consumeClick()) {
                client.setScreen(new PlaylistScreen(client.screen));
            }
        });
    }
}
