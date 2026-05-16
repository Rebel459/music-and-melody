package net.rebel459.music_and_melody.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.rebel459.music_and_melody.sound.MaMSounds;

public class WitherMusicHelper {

    public static final Music WITHER_BOSS = new Music(MaMSounds.MUSIC_WITHER, 0, 0, true);

    public static boolean hasWitherBossBar() {
        return Minecraft.getInstance().gui.getBossOverlay().events.values().stream().anyMatch(event -> event.getName().getString().equals(Component.translatable("entity.minecraft.wither").getString()));
    }
}