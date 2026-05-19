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
		boolean restoreStructureMusic = !hasExistingConfig || !configContainsField(existingConfigPath, "structure_music");
		boolean restoreBiomeMusic = !hasExistingConfig || !configContainsField(existingConfigPath, "biome_music");
		AutoConfig.register(MaMServerConfig.class, JanksonConfigSerializer::new);
		var holder = AutoConfig.getConfigHolder(MaMServerConfig.class);
		MaMServerConfig config = holder.getConfig();
		if (config.normalizeDefaults(restoreStructureMusic, restoreBiomeMusic)) {
			holder.save();
		}
	}

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean count_disc_uses = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public List<BiomeMusic> biome_music = new ArrayList<>();

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public int structure_music_min = 300;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public int structure_music_max = 600;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public List<StructureMusic> structure_music = new ArrayList<>();

	private static boolean configContainsField(Path path, String fieldName) {
		try {
			return Files.readString(path).contains("\"" + fieldName + "\"");
		} catch (Exception ignored) {
			return true;
		}
	}

	private boolean normalizeDefaults(boolean restoreStructureMusic, boolean restoreBiomeMusic) {
		boolean changed = false;

		// Structure Music

		if (restoreStructureMusic && this.structure_music.isEmpty()) {
			this.structure_music.addAll(defaultStructureMusic());
			changed = true;
		}

		LinkedHashMap<String, String> normalizedStructureMusic = new LinkedHashMap<>();
		for (StructureMusic entry : this.structure_music) {
			if (entry == null || entry.id == null || entry.id.isBlank()) continue;
			normalizedStructureMusic.remove(entry.id);
			normalizedStructureMusic.put(entry.id, entry.pool);
		}

		List<StructureMusic> normalizedStructureMusicPools = new ArrayList<>();
		normalizedStructureMusic.forEach((key, pool) -> normalizedStructureMusicPools.add(new StructureMusic(key, pool)));
		if (!sameStructureEntries(normalizedStructureMusicPools, this.structure_music)) {
			this.structure_music = normalizedStructureMusicPools;
			changed = true;
		}

		// Biome Music

		if (restoreBiomeMusic && this.biome_music.isEmpty()) {
			this.biome_music.addAll(defaultBiomeMusic());
			changed = true;
		}

		LinkedHashMap<String, String> normalizedBiomeMusic = new LinkedHashMap<>();
		for (BiomeMusic entry : this.biome_music) {
			if (entry == null || entry.key == null || entry.key.isBlank()) continue;
			normalizedBiomeMusic.remove(entry.key);
			normalizedBiomeMusic.put(entry.key, entry.pool);
		}

		List<BiomeMusic> normalizedBiomeMusicPools = new ArrayList<>();
		normalizedBiomeMusic.forEach((key, pool) -> normalizedBiomeMusicPools.add(new BiomeMusic(key, pool)));
		if (!sameBiomeEntries(normalizedBiomeMusicPools, this.biome_music)) {
			this.biome_music = normalizedBiomeMusicPools;
			changed = true;
		}

		return changed;
	}

	private static List<BiomeMusic> defaultBiomeMusic() {
		return List.of(
				new BiomeMusic("#music_and_melody:music/snowy", "music_and_melody:music.overworld.snowy"),
				new BiomeMusic("#music_and_melody:music/savanna", "music_and_melody:music.overworld.savanna"),
				new BiomeMusic("#music_and_melody:music/dark_forest", "music_and_melody:music.overworld.dark_forest"),
				new BiomeMusic("minecraft:the_end", "music_and_melody:music.end.main_island")
		);
	}

	private static List<StructureMusic> defaultStructureMusic() {
		return List.of(
				new StructureMusic("minecraft:stronghold", "music_and_melody:music.structure.stronghold"),
				new StructureMusic("minecraft:ancient_city", "music_and_melody:music.structure.ancient_city")
		);
	}

	private static boolean sameBiomeEntries(List<BiomeMusic> left, List<BiomeMusic> right) {
		if (left.size() != right.size()) return false;

		for (int i = 0; i < left.size(); i++) {
			BiomeMusic leftEntry = left.get(i);
			BiomeMusic rightEntry = right.get(i);
			if (leftEntry == rightEntry) continue;
			if (leftEntry == null || rightEntry == null) return false;
			if (!Objects.equals(leftEntry.key, rightEntry.key) || !Objects.equals(leftEntry.pool, rightEntry.pool)) return false;
		}

		return true;
	}
	private static boolean sameStructureEntries(List<StructureMusic> left, List<StructureMusic> right) {
		if (left.size() != right.size()) return false;

		for (int i = 0; i < left.size(); i++) {
			StructureMusic leftEntry = left.get(i);
			StructureMusic rightEntry = right.get(i);
			if (leftEntry == rightEntry) continue;
			if (leftEntry == null || rightEntry == null) return false;
			if (!Objects.equals(leftEntry.id, rightEntry.id) || !Objects.equals(leftEntry.pool, rightEntry.pool)) return false;
		}

		return true;
	}

	public static class BiomeMusic {
		@ConfigEntry.Gui.Tooltip
		public String key;

		@ConfigEntry.Gui.Tooltip
		public String pool;

		public BiomeMusic() {}

		public BiomeMusic(String key, String pool) {
			this.key = key;
			this.pool = pool;
		}
	}

	public static class StructureMusic {
		@ConfigEntry.Gui.Tooltip
		public String id;

		@ConfigEntry.Gui.Tooltip
		public String pool;

		public StructureMusic() {}

		public StructureMusic(String id, String pool) {
			this.id = id;
			this.pool = pool;
		}
	}
}
