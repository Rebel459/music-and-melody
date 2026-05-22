package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.rebel459.music_and_melody.MusicAndMelody;

@Config(name = MusicAndMelody.MOD_ID + "/" + "client")
public class MaMClientConfig implements ConfigData {

	public static MaMClientConfig get() {
		return AutoConfig.getConfigHolder(MaMClientConfig.class).getConfig();
	}

	public static void init() {
		AutoConfig.register(MaMClientConfig.class, JanksonConfigSerializer::new);
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

	public enum ButtonPlacement {
		TOP,
		BOTTOM,
		NONE
	}
}
