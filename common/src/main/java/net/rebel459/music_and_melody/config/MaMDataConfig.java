package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Config(name = MusicAndMelody.MOD_ID + "/" + "data")
public class MaMDataConfig implements ConfigData {

	public static MaMDataConfig get() {
		return AutoConfig.getConfigHolder(MaMDataConfig.class).getConfig();
	}

	public static void init() {
		AutoConfig.register(MaMDataConfig.class, JanksonConfigSerializer::new);
	}

	public Albums albums = new Albums();

	public static class Albums {
		public List<String> disabled_albums = new ArrayList<>();
		public List<String> disabled_tracks = new ArrayList<>();
	}

	@ConfigEntry.Gui.CollapsibleObject
	public Playlist playlist = new Playlist();

	public static class Playlist {
		public boolean loop = false;
		public List<String> queued_songs = new ArrayList<>();
	}
}
