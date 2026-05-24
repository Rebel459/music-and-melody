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
		public boolean filter_inclusive = true;
		public boolean filter_favourites = true;
		public boolean filter_albums = true;
		public boolean filter_playlists = true;
		public boolean filter_downloaded = true;
		public boolean filter_remote = true;
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
		public List<DownloadedAlbumPack> downloads = new ArrayList<>();
	}

	public static class DownloadedAlbumPack {
		public String id = "";
		public String version = "";
		public String sha256 = "";
		public String file = "";
	}

	@ConfigEntry.Gui.CollapsibleObject
	public Playlists playlists = new Playlists();

	public static class Playlists {
		public boolean loop = false;
		public List<String> queued_songs = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
	}

	@ConfigEntry.Gui.CollapsibleObject
	public Events events = new Events();

	public static class Events {
		public boolean filter_inclusive = true;
		public boolean filter_custom = true;
		public boolean filter_built_in = true;
		public boolean filter_enabled = true;
		public boolean filter_disabled = true;
		public List<String> disabled_events = new ArrayList<>();
		public List<String> enabled_events = new ArrayList<>();
	}
}
