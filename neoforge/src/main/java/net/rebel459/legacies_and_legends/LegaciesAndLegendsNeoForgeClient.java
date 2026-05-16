package net.rebel459.legacies_and_legends;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.rebel459.legacies_and_legends.config.LaLConfig;

@Mod(value = LaLConstants.MOD_ID, dist = Dist.CLIENT)
public class LegaciesAndLegendsNeoForgeClient {

    public LegaciesAndLegendsNeoForgeClient(IEventBus modEventBus) {
        LegaciesAndLegendsClient.initRegistries();
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (modContainer, parent) ->
                        AutoConfigClient.getConfigScreen(LaLConfig.class, parent).get()
        );
        modEventBus.addListener(LegaciesAndLegendsNeoForgeClient::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        LegaciesAndLegendsClient.init();
    }
}