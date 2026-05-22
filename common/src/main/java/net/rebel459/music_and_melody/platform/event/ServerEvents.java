package net.rebel459.music_and_melody.platform.event;

import net.minecraft.server.MinecraftServer;
import net.rebel459.music_and_melody.platform.util.EventType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ServerEvents {

    private static final List<Consumer<MinecraftServer>> DATAPACK_RELOAD_ENTRIES = new CopyOnWriteArrayList<>();

    public static void onDatapackLoad(Consumer<MinecraftServer> handler) {
        DATAPACK_RELOAD_ENTRIES.add(handler);
    }

    public static void passOnDatapackLoad(MinecraftServer server) {
        for (Consumer<MinecraftServer> listener : DATAPACK_RELOAD_ENTRIES) {
            listener.accept(server);
        }
    }

    private static final EnumMap<EventType, List<Consumer<MinecraftServer>>> TICK_LISTENERS = new EnumMap<>(Map.of(
            EventType.PRE, new ArrayList<>(),
            EventType.POST, new ArrayList<>()
    ));

    public static void onTick(EventType type, Consumer<MinecraftServer> listener) {
        TICK_LISTENERS.get(type).add(listener);
    }

    public static void passOnTick(EventType type, MinecraftServer server) {
        for (Consumer<MinecraftServer> listener : TICK_LISTENERS.get(type)) {
            listener.accept(server);
        }
    }
}