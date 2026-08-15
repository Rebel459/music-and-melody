package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.resources.Identifier;
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
		MaMDataConfig config = AutoConfig.getConfigHolder(MaMDataConfig.class).getConfig();
		config.migrateDownloadedPacks();
		return config;
	}
	public Albums albums = new Albums();

	public static class Albums {
		public BrowserTab browser_tab = BrowserTab.ALBUMS;
		public boolean favourites_first = true;
		public boolean downloads_first = true;
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
		public List<String> favourites = new ArrayList<>();
		/** @deprecated Downloads are stored under {@link Remote}; retained only for one-time migration. */
		@Deprecated
		public List<DownloadedPack> downloads = new ArrayList<>();
	}

	public enum BrowserTab {
		ALBUMS,
		PLAYLISTS,
		REMOTE
	}

	public static class DownloadedPack {
		public String id = "";
		public String tag = "";
		public String version = "";
		public String sha256 = "";
		public String file = "";
	}

	public Playlists playlists = new Playlists();

	public static class Playlists {
		public boolean loop = false;
		public boolean shuffle = false;
		public QueueSourceType queue_source_type = QueueSourceType.NONE;
		public String queue_source_id = "";
		public String queue_source_name = "";
		public List<String> queued_songs = new ArrayList<>();
		public List<String> custom_playlist_songs = new ArrayList<>();
		public boolean custom_playlist_migrated = false;
		public List<String> favourites = new ArrayList<>();
		/**
		 * Most-recently played source identifiers, stored as {@code TYPE|identifier}.
		 * Keeping this separate from favourites means an item can retain its position
		 * when it is unfavourited and favourited again later.
		 */
		public List<String> recent_sources = new ArrayList<>();
	}

	public enum QueueSourceType {
		NONE,
		ALBUM,
		PLAYLIST
	}

	public Events events = new Events();

	public static class Events {
		public boolean enabled_first = true;
		public boolean show_custom = true;
		public boolean show_built_in = true;
		public List<String> disabled_events = new ArrayList<>();
		public List<String> enabled_events = new ArrayList<>();
	}

	public Remote remote = new Remote();
	
	public static class Remote {
		public List<String> added_repositories = new ArrayList<>();
		public boolean official_provider = true;
		public boolean community_provider = true;
		public List<DownloadedPack> downloads = new ArrayList<>();
	}

	public String active_theme = "music_and_melody:default";
	
	public float gui_multiplier = 0.85F;

	private void migrateDownloadedPacks() {
		if (this.remote == null) this.remote = new Remote();
		if (this.remote.downloads == null) this.remote.downloads = new ArrayList<>();
		if (this.albums == null || this.albums.downloads == null || this.albums.downloads.isEmpty()) return;
		for (DownloadedPack legacy : this.albums.downloads) {
			if (legacy == null || legacy.id == null || legacy.id.isBlank()) continue;
			boolean exists = this.remote.downloads.stream().anyMatch(current -> current != null
					&& current.id.equals(legacy.id) && current.tag.equalsIgnoreCase(legacy.tag));
			if (!exists) this.remote.downloads.add(legacy);
		}
		this.albums.downloads.clear();
		AutoConfig.getConfigHolder(MaMDataConfig.class).save();
	}
}
