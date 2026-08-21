package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
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
	public Albums albums = new Albums();

	public static class Albums {
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
	}

	public static class DownloadedPack {
		public String id = "";
		public List<String> tags = new ArrayList<>();
		public String version = "";
		public String sha256 = "";
		public String file = "";
	}

	public Playlist playlist = new Playlist();

	public static class Playlist {
		public boolean loop = false;
		public boolean shuffle = false;
		public QueueType queue_type = QueueType.NONE;
		public String queue_id = "";
		public String queue_name = "";
		public List<String> queued_songs = new ArrayList<>();
		public List<String> custom_playlist_songs = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
		public List<String> recent_favourites = new ArrayList<>();
	}

	public enum QueueType {
		NONE,
		ALBUM,
		PLAYLIST
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
