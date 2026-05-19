package net.rebel459.music_and_melody.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.rebel459.music_and_melody.sound.MaMSounds;

public class MusicHelper {

    public static final Music WITHER_BOSS = createEventMusic(MaMSounds.MUSIC_WITHER);
    public static final Music THRESHOLD = createEventMusic(MaMSounds.MUSIC_THRESHOLD);

    public static Music createEventMusic(Holder<SoundEvent> music) {
        return new Music(music, 0, 0, true);
    }

    public static boolean hasWitherBossBar() {
        return Minecraft.getInstance().gui.getBossOverlay().events.values().stream().anyMatch(event -> event.getName().getString().equals(Component.translatable("entity.minecraft.wither").getString()));
    }

    public static boolean isEndPortalFilled() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        return manager.soundEngine.instanceBySource.values().stream().filter(instance -> SoundEvents.END_PORTAL_SPAWN.location().equals(instance.getIdentifier())).anyMatch(manager::isActive);
    }
}