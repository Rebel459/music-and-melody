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
	public boolean creative_fix = true;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean under_water_fix = true;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean event_weight_fix = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean wither_music = true;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean end_portal_music = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean common_music = false;
	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int common_music_chance = 50;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean background_music = true;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public ButtonPosition button_positions = ButtonPosition.SOUNDS;

	@ConfigEntry.Category("config")
	@ConfigEntry.Gui.Tooltip
	public boolean custom_album = true;

	public enum ButtonPosition {
		SOUNDS,
		OPTIONS,
		NONE
	}
}
