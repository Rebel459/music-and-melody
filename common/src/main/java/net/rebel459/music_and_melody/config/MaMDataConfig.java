package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.ArrayList;
import java.util.List;

@Config(name = MusicAndMelody.MOD_ID + "/" + "data")
public class MaMDataConfig implements ConfigData {

	public static MaMDataConfig get() {
		if (!MusicAndMelody.registeredDataConfig) {
			AutoConfig.register(MaMDataConfig.class, JanksonConfigSerializer::new);
			MusicAndMelody.registeredDataConfig = true;
		}
		return AutoConfig.getConfigHolder(MaMDataConfig.class).getConfig();
	}

	public Player player = new Player();

	public static class Player {
		public boolean loop = false;
		public boolean shuffle = false;
		public NowPlayingType now_playing_type = NowPlayingType.NONE;
		public String now_playing_id = "";
		public String now_playing_name = "";
		public List<String> custom_playlist_tracks = new ArrayList<>();
		public List<String> custom_playlist_discs = new ArrayList<>();
		public List<Entry> favourites = new ArrayList<>();
		public List<Entry> recent_favourites = new ArrayList<>();
	}

	public enum NowPlayingType {
		NONE,
		ALBUM,
		PLAYLIST
	}

	public static class Entry {
		public String id = "";
		public String type = "";

		public boolean isAlbum() {
			return "album".equals(type);
		}
		public boolean isPlaylist() {
			return "playlist".equals(type);
		}
		public boolean isTrack() {
			return "track".equals(type);
		}
		public boolean isDisc() {
			return "disc".equals(type);
		}
	}

	public Albums albums = new Albums();

	public static class Albums {
		public List<String> disabled = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
	}

	public Events events = new Events();

	public static class Events {
		public List<String> disabled = new ArrayList<>();
		public List<String> enabled = new ArrayList<>();
	}

	public Remote remote = new Remote();

	public static class Remote {
		public List<String> catalogs = new ArrayList<>();
		public boolean official_provider = true;
		public boolean community_provider = true;
		public List<DownloadedPack> downloads = new ArrayList<>();
	}

	public static class DownloadedPack {
		public String id = "";
		public List<String> tags = new ArrayList<>();
		public String version = "";
		public long size = 0L;
		public String file = "";
	}

	public Cache cache = new Cache();

	public static class Cache {
		public String splash = "";
		public String supporter = "";
		public String composer = "";
	}

	public boolean event_music = true;
	public boolean vanilla_music = true;

	public String active_theme = "music_and_melody:default";
	
	public float gui_multiplier = 0.85F;

	@Override
	public void validatePostLoad() {
		if (player == null) player = new Player();
		if (player.now_playing_type == null) player.now_playing_type = NowPlayingType.NONE;
		if (player.now_playing_id == null) player.now_playing_id = "";
		if (player.now_playing_name == null) player.now_playing_name = "";
		if (player.custom_playlist_tracks == null) player.custom_playlist_tracks = new ArrayList<>();
		if (player.custom_playlist_discs == null) player.custom_playlist_discs = new ArrayList<>();
		if (player.favourites == null) player.favourites = new ArrayList<>();
		if (player.recent_favourites == null) player.recent_favourites = new ArrayList<>();
		if (albums == null) albums = new Albums();
		if (albums.disabled == null) albums.disabled = new ArrayList<>();
		if (albums.disabled_tracks == null) albums.disabled_tracks = new ArrayList<>();
		if (events == null) events = new Events();
		if (events.disabled == null) events.disabled = new ArrayList<>();
		if (events.enabled == null) events.enabled = new ArrayList<>();
		if (remote == null) remote = new Remote();
		if (remote.catalogs == null) remote.catalogs = new ArrayList<>();
		if (remote.downloads == null) remote.downloads = new ArrayList<>();
		if (cache == null) cache = new Cache();
		if (cache.splash == null) cache.splash = "";
		if (cache.supporter == null) cache.supporter = "";
		if (cache.composer == null) cache.composer = "";
		if (active_theme == null || active_theme.isBlank()) active_theme = "music_and_melody:default";
		if (!Float.isFinite(gui_multiplier) || gui_multiplier < 0.25F) gui_multiplier = 0.85F;
		gui_multiplier = Math.min(gui_multiplier, 4.0F);
	}
}
