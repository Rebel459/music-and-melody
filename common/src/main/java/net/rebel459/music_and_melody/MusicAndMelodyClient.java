package net.rebel459.music_and_melody;

import net.rebel459.music_and_melody.config.MaMConfig;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.util.PackType;

public final class MusicAndMelodyClient {

    public static void initRegistries() {}

    public static void init() {
        if (MaMConfig.get().client.music_rebalance) {
            UnifiedHelpers.PACKS.add(MusicAndMelody.id("music_and_melody"), PackType.REQUIRED_RESOURCES);
        }
        if (MaMConfig.get().client.end_portal_music) {
            UnifiedHelpers.PACKS.add(MusicAndMelody.id("end_portal_music"), PackType.REQUIRED_RESOURCES);
        }
    }
}
