package net.rebel459.music_and_melody.platform.client.event;

import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.platform.util.EventType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InstanceEvents {

        private InstanceEvents() {}

        private static final EnumMap<EventType, List<Consumer<Minecraft>>> TICK_LISTENERS = new EnumMap<>(Map.of(
                EventType.PRE, new ArrayList<>(),
                EventType.POST, new ArrayList<>()
        ));

        public static void onTick(EventType type, Consumer<Minecraft> listener) {
            TICK_LISTENERS.get(type).add(listener);
        }

        public static void passOnTick(EventType type, Minecraft client) {
            for (Consumer<Minecraft> listener : TICK_LISTENERS.get(type)) {
                listener.accept(client);
            }
        }
    }