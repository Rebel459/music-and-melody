package net.rebel459.music_and_melody.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.config.MaMConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Album {

    public static Set<Album> ALBUMS = new HashSet<>();
    public static Set<Album> DISABLED_ALBUMS = new HashSet<>();

    public Identifier album;
    public Component name;
    public Identifier icon;
    public int composers;
    public List<String> songs;

    public Album(Identifier album, Component name, Identifier icon, int composers, List<String> songs) {
        this.album = album;
        this.name = name;
        this.icon = icon;
        this.composers = composers;
        this.songs = songs;
        ALBUMS.add(this);
        if (!isEnabled()) DISABLED_ALBUMS.add(this);
    }

    public boolean isEnabled() {
        return !MaMConfig.get().client.albums.disabled_albums.contains(this.album.toString());
    }

    public void setEnabled(boolean enabled) {
        String id = this.album.toString();
        MaMConfig config = MaMConfig.get();

        if (enabled) {
            config.client.albums.disabled_albums.remove(id);
            DISABLED_ALBUMS.remove(this);
        } else {
            if (!config.client.albums.disabled_albums.contains(id)) {
                config.client.albums.disabled_albums.add(id);
            }
            DISABLED_ALBUMS.add(this);
        }

        AutoConfig.getConfigHolder(MaMConfig.class).save();
    }

    public Identifier trackId(String song) {
        return Identifier.fromNamespaceAndPath(this.album.getNamespace(), song);
    }

    public boolean isTrackEnabled(String song) {
        return !MaMConfig.get().client.albums.disabled_tracks.contains(trackId(song).toString());
    }

    public void setTrackEnabled(String song, boolean enabled) {
        String id = trackId(song).toString();
        MaMConfig config = MaMConfig.get();

        if (enabled) {
            config.client.albums.disabled_tracks.remove(id);
        } else if (!config.client.albums.disabled_tracks.contains(id)) {
            config.client.albums.disabled_tracks.add(id);
        }

        AutoConfig.getConfigHolder(MaMConfig.class).save();
    }

    public record Record(Component name, Identifier icon, int composers, List<String> songs) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("composers", 0).forGetter(Record::composers),
                ExtraCodecs.NON_EMPTY_STRING.listOf().optionalFieldOf("songs", List.of()).forGetter(Record::songs)
        ).apply(instance, Record::new));
    }
}
