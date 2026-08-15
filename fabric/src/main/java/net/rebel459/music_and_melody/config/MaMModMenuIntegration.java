package net.rebel459.music_and_melody.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;

@Environment(EnvType.CLIENT)
public final class MaMModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return screen -> new MusicPlayerScreen(screen, MusicPlayerScreen.Page.CONFIG);
	}
}
