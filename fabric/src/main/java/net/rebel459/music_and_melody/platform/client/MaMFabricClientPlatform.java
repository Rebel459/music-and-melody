package net.rebel459.music_and_melody.platform.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.platform.client.event.InstanceEvents;
import net.rebel459.music_and_melody.platform.client.registry.KeyMappingRegistry;
import net.rebel459.music_and_melody.platform.util.EventType;

import java.util.function.Supplier;

public class MaMFabricClientPlatform {

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register((client) -> InstanceEvents.passOnTick(EventType.PRE, client));
        ClientTickEvents.END_CLIENT_TICK.register((client) -> InstanceEvents.passOnTick(EventType.POST, client));
        MaMClientPlatform.KEY_MAPPINGS = new FabricKeyMappingRegistry();
    }

    public static class FabricKeyMappingRegistry implements KeyMappingRegistry {

        @Override
        public Supplier<KeyMapping> registerKeybind(String path, InputConstants.Type type, Integer key, KeyMapping.Category category) {
            var keyBind = Suppliers.memoize(() -> KeyBindingHelper.registerKeyBinding(
                    new KeyMapping(
                            "key." + MusicAndMelody.MOD_ID + "." + path,
                            type,
                            key,
                            category
                    ))
            );
            keyBind.get();
            return keyBind;
        }
    }
}
