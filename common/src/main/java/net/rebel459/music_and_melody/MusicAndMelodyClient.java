package net.rebel459.music_and_melody;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;
import net.rebel459.music_and_melody.platform.client.MaMClientPlatform;
import net.rebel459.music_and_melody.platform.client.event.InstanceEvents;
import net.rebel459.music_and_melody.platform.util.EventType;
import net.rebel459.music_and_melody.platform.util.PackType;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public final class MusicAndMelodyClient {

    public static final Supplier<KeyMapping> PLAYLIST_KEY = MaMClientPlatform.KEY_MAPPINGS.registerKeybind(
            "playlist",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.CATEGORY_MISC
    );

    public static void initRegistries() {
        if (MaMClientConfig.get().music_rebalance) {
            MaMPlatform.PACKS.add(MusicAndMelody.id("music_and_melody"), PackType.REQUIRED_RESOURCES);
        }
    }

    public static void init() {
        InstanceEvents.onTick(EventType.POST, client -> {
            while (PLAYLIST_KEY.get().consumeClick()) {
                client.setScreen(new PlaylistScreen(client.screen));
            }
        });
    }
}
