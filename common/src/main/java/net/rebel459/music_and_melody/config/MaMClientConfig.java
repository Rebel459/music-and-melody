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

		Path json5Path = configPath(true);
		Path jsonPath = configPath(false);

		Path existingConfigPath = Files.exists(json5Path) ? json5Path : jsonPath;
		boolean hasExistingConfig = Files.exists(existingConfigPath);

		boolean restoreRemoteRepositories =
				!hasExistingConfig || !configContainsField(existingConfigPath, "remote_repositories");

		AutoConfig.register(MaMClientConfig.class, JanksonConfigSerializer::new);
		MusicAndMelody.registeredClientConfig = true;

		var holder = AutoConfig.getConfigHolder(MaMClientConfig.class);
		MaMClientConfig config = holder.getConfig();

		if (config.normalizeDefaults(restoreRemoteRepositories)) {
			holder.save();
		}
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
	public boolean pool_weight_fix = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public ButtonPlacement button_placement = ButtonPlacement.TOP;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean config_album = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.RequiresRestart
	public boolean allow_events = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean vanilla_music = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean remote_downloads = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public List<String> remote_repositories = new ArrayList<>();

	public enum ButtonPlacement {
		TOP,
		BOTTOM,
		NONE
	}

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

	private boolean normalizeDefaults(boolean restoreRemoteRepositories) {
		boolean changed = false;

		if (this.remote_repositories == null) {
			this.remote_repositories = new ArrayList<>();
			changed = true;
		}

		List<String> normalizedRemoteRepositories = this.remote_repositories.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(repository -> !repository.isEmpty())
				.distinct()
				.collect(Collectors.toCollection(ArrayList::new));

		if (restoreRemoteRepositories && normalizedRemoteRepositories.isEmpty()) {
			normalizedRemoteRepositories.addAll(defaultRemoteRepositories());
		}

		if (!normalizedRemoteRepositories.equals(this.remote_repositories)) {
			this.remote_repositories = normalizedRemoteRepositories;
			changed = true;
		}

		return changed;
	}

	private static List<String> defaultRemoteRepositories() {
		return List.of(
				"https://github.com/Rebel459/music-and-melody-remote"
		);
	}
}