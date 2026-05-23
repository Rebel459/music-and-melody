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

	private static boolean registered = false;

	public static MaMDataConfig get() {
		if (!registered) {
			AutoConfig.register(MaMDataConfig.class, JanksonConfigSerializer::new);
			registered = true;
		}
		return AutoConfig.getConfigHolder(MaMDataConfig.class).getConfig();
	}
	public Albums albums = new Albums();

	public static class Albums {
		public AlbumDisplay display = AlbumDisplay.ALL;
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
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
		public EventDisplay display = EventDisplay.ALL;
		public List<String> disabled_events = new ArrayList<>();
		public List<String> enabled_events = new ArrayList<>();
	}

	public enum AlbumDisplay {
		ALL,
		ALBUMS,
		PLAYLISTS,
		FAVOURITES
	}

	public enum EventDisplay {
		ALL,
		ENABLED,
		DISABLED,
		CUSTOM,
		BUILT_IN
	}
}
