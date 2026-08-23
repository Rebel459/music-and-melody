package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.rebel459.music_and_melody.MusicAndMelody;

@Config(name = MusicAndMelody.MOD_ID + "/" + "server")
public class MaMServerConfig implements ConfigData {

	public static MaMServerConfig get() {
		if (!MusicAndMelody.registeredServerConfig) {
			AutoConfig.register(MaMServerConfig.class, JanksonConfigSerializer::new);
			MusicAndMelody.registeredServerConfig = true;
		}
		return AutoConfig.getConfigHolder(MaMServerConfig.class).getConfig();
	}

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean disc_unlocking = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean sync_structures = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean improved_pve_detection = true;
}
