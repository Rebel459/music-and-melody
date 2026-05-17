package net.rebel459.music_and_melody.client;

import net.minecraft.resources.Identifier;

import java.util.*;

public class Album {

    public static Set<Album> ALBUMS = new HashSet<>();

    public Identifier album;
    public boolean enabled;
    public Optional<List<Identifier>> songs;
    public Optional<String> namespace;

    public Album(Identifier album, boolean enabled) {
        this(album, enabled, Optional.empty(), Optional.of(album.getNamespace()));
    }

    public Album(Identifier album, List<Identifier> songs) {
        this(album, true, songs);
    }

    public Album(Identifier album, boolean enabled, List<Identifier> songs) {
        this(album, enabled, Optional.of(songs), Optional.empty());
    }

    public Album(Identifier album, boolean enabled, Optional<List<Identifier>> songs, Optional<String> namespace) {
        this.album = album;
        this.enabled = enabled;
        this.songs = songs;
        this.namespace = namespace;
        ALBUMS.add(this);
    }
}
