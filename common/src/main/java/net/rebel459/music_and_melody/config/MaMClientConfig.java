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
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Config(name = MusicAndMelody.MOD_ID + "/" + "client")
public class MaMClientConfig implements ConfigData {

	public static MaMClientConfig get() {
		init();
		return AutoConfig.getConfigHolder(MaMClientConfig.class).getConfig();
	}

	@Contract(pure = true)
	public static @NotNull Path configPath(boolean json5) {
		return Path.of("./config/" + MusicAndMelody.MOD_ID + "/client." + (json5 ? "json5" : "json"));
	}

	private static void init() {
		if (MusicAndMelody.registeredClientConfig) return;
		AutoConfig.register(MaMClientConfig.class, JanksonConfigSerializer::new);
		MusicAndMelody.registeredClientConfig = true;
	}

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean music_rebalance = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean jukebox_fading = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public float fade_speed = 0.01F;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean creative_fix = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean under_water_fix = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean pool_weight_fix = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public boolean menu_buttons = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean automatic_discs = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.RequiresRestart
	public boolean allow_events = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean vanilla_music = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean discord_button = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean kofi_button = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean remote_downloads = true;

	private static boolean configContainsField(Path path, String fieldName) {
		try {
			String config = Files.readString(path);

			return Pattern.compile("(?m)^\\s*\"?" + Pattern.quote(fieldName) + "\"?\\s*:")
					.matcher(config)
					.find();
		} catch (Exception ignored) {
			return true;
		}
	}
}