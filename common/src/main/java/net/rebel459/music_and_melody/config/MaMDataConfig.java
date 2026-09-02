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
		public String sha256 = "";
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
}
