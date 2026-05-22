package net.rebel459.music_and_melody.platform.client.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.function.Supplier;

public interface KeyMappingRegistry {
    Supplier<KeyMapping> registerKeybind(String path, InputConstants.Type type, Integer key, String category);
}