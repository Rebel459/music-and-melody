package net.rebel459.music_and_melody.platform.event;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PlayerEvents {

        private PlayerEvents() {}

        private static final List<Consumer<ServerPlayer>> JOIN_LISTENERS = new CopyOnWriteArrayList<>();

        public static void onJoin(Consumer<ServerPlayer> listener) {
            JOIN_LISTENERS.add(listener);
        }

        static void passOnJoin(ServerPlayer player) {
            for (Consumer<ServerPlayer> listener : JOIN_LISTENERS) {
                listener.accept(player);
            }
        }
    }