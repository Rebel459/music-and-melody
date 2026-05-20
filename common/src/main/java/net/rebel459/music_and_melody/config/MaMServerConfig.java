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
import java.util.*;

@Config(name = MusicAndMelody.MOD_ID + "/" + "server")
public class MaMServerConfig implements ConfigData {

	@Contract(pure = true)
	public static @NotNull Path configPath(boolean json5) {
		return Path.of("./config/" + MusicAndMelody.MOD_ID + "/server." + (json5 ? "json5" : "json"));
	}

	public static MaMServerConfig get() {
		return AutoConfig.getConfigHolder(MaMServerConfig.class).getConfig();
	}

	public static void init() {
		Path json5Path = configPath(true);
		Path jsonPath = configPath(false);
		Path existingConfigPath = Files.exists(json5Path) ? json5Path : jsonPath;
		boolean hasExistingConfig = Files.exists(existingConfigPath);
		boolean restoreSoundEvents = !hasExistingConfig || !configContainsField(existingConfigPath, "sound_events");
		AutoConfig.register(MaMServerConfig.class, JanksonConfigSerializer::new);
		var holder = AutoConfig.getConfigHolder(MaMServerConfig.class);
		MaMServerConfig config = holder.getConfig();
		if (config.normalizeDefaults(restoreSoundEvents)) {
			holder.save();
		}
	}

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean count_disc_uses = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean sync_structures = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public List<String> sound_events = new ArrayList<>();

	private static boolean configContainsField(Path path, String fieldName) {
		try {
			return Files.readString(path).contains("\"" + fieldName + "\"");
		} catch (Exception ignored) {
			return true;
		}
	}

	private boolean normalizeDefaults(boolean restoreSoundEvents) {
		boolean changed = false;

		if (restoreSoundEvents && this.sound_events.isEmpty()) {
			this.sound_events.addAll(defaultSoundEvents());
			changed = true;
		}

		List<String> normalizedDisabledEnchantments = new ArrayList<>(new LinkedHashSet<>(this.sound_events));
		if (!normalizedDisabledEnchantments.equals(this.sound_events)) {
			this.sound_events = normalizedDisabledEnchantments;
			changed = true;
		}

		return changed;
	}

	private static List<String> defaultSoundEvents() {
		return List.of();
	}
}
