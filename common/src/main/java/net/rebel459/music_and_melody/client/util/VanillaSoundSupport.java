package net.rebel459.music_and_melody.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaSoundSupport {

    private static final Map<Identifier, SafeIdentifier> SOURCES = new ConcurrentHashMap<>();

    private VanillaSoundSupport() {}

    public static void prepare(JsonElement json) {
        if (!json.isJsonObject()) return;
        JsonArray sounds = json.getAsJsonObject().getAsJsonArray("sounds");
        if (sounds == null) return;

        for (int index = 0; index < sounds.size(); index++) {
            JsonElement element = sounds.get(index);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                sounds.set(index, prepareName(element.getAsString()));
            } else if (element.isJsonObject()) {
                JsonObject sound = element.getAsJsonObject();
                if (sound.has("type") && !"file".equals(sound.get("type").getAsString())) continue;
                if (!sound.has("name") || !sound.get("name").isJsonPrimitive()) continue;
                sound.add("name", prepareName(sound.get("name").getAsString()));
            }
        }
    }

    private static JsonPrimitive prepareName(String name) {
        SafeIdentifier source = SafeIdentifier.parse(name);
        Identifier parsed = Identifier.tryParse(name);
        if (parsed != null && !hasAudioExtension(source.getPath())) return new JsonPrimitive(name);

        Identifier runtime = parsed != null ? parsed : syntheticId(name);
        SOURCES.put(runtime, source);
        return new JsonPrimitive(runtime.toString());
    }

    public static Optional<SafeIdentifier> source(Identifier runtime) {
        return Optional.ofNullable(SOURCES.get(runtime));
    }

    public static void clear() {
        SOURCES.clear();
    }

    private static boolean hasAudioExtension(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".ogg") || lower.endsWith(".mp3")
                || lower.endsWith(".flac") || lower.endsWith(".wav");
    }

    private static Identifier syntheticId(String name) {
        UUID hash = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return MusicAndMelody.id("sounds_json/" + hash);
    }
}
