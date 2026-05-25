package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
		public boolean favourites_only = false;
		public boolean show_albums = true;
		public boolean show_playlists = true;
		public boolean show_remote = true;
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
		public EventVisibility visibility = EventVisibility.ALL;
		public boolean show_custom = true;
		public boolean show_built_in = true;
		public List<String> disabled_events = new ArrayList<>();
		public List<String> enabled_events = new ArrayList<>();
	}

	public enum EventVisibility {
		ALL,
		ENABLED,
		DISABLED;

		public Component component() {
			return Component.translatable("screen.music_and_melody.event_filter.visibility." + this.name().toLowerCase(Locale.ROOT));
		}
	}
}
