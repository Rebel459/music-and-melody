package net.rebel459.music_and_melody.platform.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.platform.client.event.InstanceEvents;
import net.rebel459.music_and_melody.platform.client.registry.KeyMappingRegistry;
import net.rebel459.music_and_melody.platform.util.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MaMNeoForgeClientPlatform {

    public static void init(IEventBus bus) {
        bus.addListener(NeoForgeKeyMappingRegistry::registerBindings);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> {
            InstanceEvents.passOnTick(EventType.PRE, Minecraft.getInstance());
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            InstanceEvents.passOnTick(EventType.POST, Minecraft.getInstance());
        });
        MaMClientPlatform.KEY_MAPPINGS = new NeoForgeKeyMappingRegistry();
    }

    public static class NeoForgeKeyMappingRegistry implements KeyMappingRegistry {
        public static List<Supplier<KeyMapping>> MAPPINGS = new ArrayList<>();

        @Override
        public Supplier<KeyMapping> registerKeybind(String path, InputConstants.Type type, Integer key, KeyMapping.Category category) {
            Supplier<KeyMapping> keyMapping = Suppliers.memoize(() -> new KeyMapping(
                    "key." + MusicAndMelody.MOD_ID + "." + path,
                    type,
                    key,
                    category
            ));
            MAPPINGS.add(keyMapping);
            return keyMapping;
        }

        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            for (Supplier<KeyMapping> keyMapping : MAPPINGS) {
                event.register(keyMapping.get());
            }
        }
    }
}
