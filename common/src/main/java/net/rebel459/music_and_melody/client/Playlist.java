package net.rebel459.music_and_melody.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Playlist {

    public static Set<Playlist> PLAYLISTS = new HashSet<>();

    public Identifier playlist;
    public Component name;
    public Identifier icon;
    public List<Identifier> tracks;
    public List<Identifier> discs;

    public Playlist(Identifier playlist, Component name, Identifier icon, List<Identifier> tracks, List<Identifier> discs) {
        this.playlist = playlist;
        this.name = name;
        this.icon = icon;
        this.tracks = tracks;
        this.discs = discs;
        PLAYLISTS.add(this);
    }

    public record Record(Component name, Identifier icon, List<Track> tracks, List<Disc> discs) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Track.CODEC.listOf().optionalFieldOf("track_entries", List.of()).forGetter(Record::tracks),
                Disc.CODEC.listOf().optionalFieldOf("disc_entries", List.of()).forGetter(Record::discs)
        ).apply(instance, Record::new));
    }

    public record Track(String namespace, List<String> tracks) {
        private static final Codec<Track> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("namespace").forGetter(Track::namespace),
                Codec.STRING.listOf().fieldOf("tracks").forGetter(Track::tracks)
        ).apply(instance, Track::new));
    }

    public record Disc(String namespace, List<String> discs) {
        private static final Codec<Disc> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("namespace").forGetter(Disc::namespace),
                Codec.STRING.listOf().fieldOf("discs").forGetter(Disc::discs)
        ).apply(instance, Disc::new));
    }
}
