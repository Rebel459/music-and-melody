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
		public BrowserTab browser_tab = BrowserTab.ALBUMS;
		public boolean favourites_first = true;
		public boolean downloads_first = true;
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
		public List<DownloadedPack> downloads = new ArrayList<>();
	}

	public enum BrowserTab {
		ALBUMS,
		PLAYLISTS,
		REMOTE
	}

	public static class DownloadedPack {
		public String id = "";
		public String version = "";
		public String sha256 = "";
		public String file = "";
	}

	@ConfigEntry.Gui.CollapsibleObject
	public Playlists playlists = new Playlists();

	public static class Playlists {
		public boolean loop = false;
		public QueueSourceType queue_source_type = QueueSourceType.NONE;
		public String queue_source_id = "";
		public String queue_source_name = "";
		public List<String> queued_songs = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
	}

	public enum QueueSourceType {
		NONE,
		ALBUM,
		PLAYLIST
	}

	@ConfigEntry.Gui.CollapsibleObject
	public Events events = new Events();

	public static class Events {
		public boolean enabled_first = true;
		public boolean show_custom = true;
		public boolean show_built_in = true;
		public List<String> disabled_events = new ArrayList<>();
		public List<String> enabled_events = new ArrayList<>();
	}
}
