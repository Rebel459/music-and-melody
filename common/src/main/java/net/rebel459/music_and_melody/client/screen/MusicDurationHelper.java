package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.util.DirectSoundFiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;

/** Small OGG/Vorbis duration reader used by the non-seekable player timeline. */
final class MusicDurationHelper {

    /** Successful reads only: resources may not be registered on the first frame. */
    private static final Map<Identifier, Long> DURATIONS = new ConcurrentHashMap<>();
    /** Duration probes run off the render thread; a long OGG must not stall the player UI. */
    private static final Map<Identifier, CompletableFuture<Optional<Long>>> PENDING = new ConcurrentHashMap<>();
    private static final Map<Identifier, Long> RETRY_AFTER = new ConcurrentHashMap<>();
    private static final long FAILED_PROBE_RETRY_DELAY = 1_000L;

    private MusicDurationHelper() {}

    static Optional<Long> currentDurationMillis(Minecraft minecraft, SoundInstance instance) {
        if (minecraft == null || instance == null) return Optional.empty();
        Sound sound = instance.getSound();
        Identifier instanceId = instance.getIdentifier();
        if (sound == null || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            return durationForCandidates(minecraft, candidateLocations(instanceId, null));
        }
        // The sound engine uses both a logical event id and a stream path. A
        // file sound normally resolves to assets/<ns>/sounds/<path>.ogg, while
        // direct files retain both generated aliases. Probe those forms so the
        // timeline also works for vanilla/resource-pack sounds.
        List<Identifier> locations = candidateLocations(sound.getPath(), sound.getLocation());
        addCandidateForms(locations, instanceId);
        return durationForCandidates(minecraft, locations);
    }

    private static Optional<Long> durationForCandidates(Minecraft minecraft, List<Identifier> locations) {
        long now = Util.getMillis();
        for (Identifier location : locations) {
            Long cached = DURATIONS.get(location);
            if (cached != null) return Optional.of(cached);

            Long retryAfter = RETRY_AFTER.get(location);
            if (retryAfter != null && retryAfter > now) continue;
            CompletableFuture<Optional<Long>> pending = PENDING.computeIfAbsent(location,
                    candidate -> CompletableFuture.supplyAsync(() -> readDuration(minecraft, candidate), Util.nonCriticalIoPool()));
            Optional<Long> duration = pending.getNow(Optional.empty());
            if (duration.isPresent()) {
                DURATIONS.put(location, duration.get());
                PENDING.remove(location, pending);
                RETRY_AFTER.remove(location);
                return duration;
            }
            if (pending.isDone()) {
                PENDING.remove(location, pending);
                RETRY_AFTER.put(location, now + FAILED_PROBE_RETRY_DELAY);
            }
        }
        return Optional.empty();
    }

    private static List<Identifier> candidateLocations(Identifier path, Identifier logical) {
        List<Identifier> locations = new ArrayList<>();
        addCandidateForms(locations, path);
        if (logical != null && !logical.equals(path)) addCandidateForms(locations, logical);
        return locations;
    }

    private static void addCandidateForms(List<Identifier> locations, Identifier location) {
        if (location == null) return;
        addUnique(locations, location);

        String path = location.getPath();
        boolean isSoundPath = path.startsWith("sounds/");
        boolean isOgg = path.endsWith(".ogg");
        if (!isSoundPath) addUnique(locations, Identifier.fromNamespaceAndPath(location.getNamespace(), "sounds/" + path));
        if (!isOgg) {
            addUnique(locations, Identifier.fromNamespaceAndPath(location.getNamespace(), path + ".ogg"));
            if (!isSoundPath) addUnique(locations, Identifier.fromNamespaceAndPath(location.getNamespace(), "sounds/" + path + ".ogg"));
        }
    }

    private static void addUnique(List<Identifier> locations, Identifier location) {
        if (!locations.contains(location)) locations.add(location);
    }

    private static Optional<Long> readDuration(Minecraft minecraft, Identifier location) {
        try (InputStream input = open(minecraft, location)) {
            if (input == null) return Optional.empty();
            Optional<Long> indexedDuration = readOggDuration(input);
            if (indexedDuration.isPresent()) return indexedDuration;
        } catch (Exception ignored) {
        }
        // Some valid resource packs are tolerant enough for Minecraft's
        // decoder yet use OGG page layouts that are awkward to index by hand.
        // Decoding once is a slower fallback, but is still off-thread and
        // produces the exact PCM duration used by the sound engine.
        return decodeDuration(minecraft, location);
    }

    private static Optional<Long> decodeDuration(Minecraft minecraft, Identifier location) {
        InputStream opened;
        try {
            opened = open(minecraft, location);
        } catch (IOException ignored) {
            return Optional.empty();
        }
        if (opened == null) return Optional.empty();
        try (InputStream input = opened; JOrbisAudioStream stream = new JOrbisAudioStream(input)) {
            AudioFormat format = stream.getFormat();
            int frameSize = Math.max(1, format.getFrameSize());
            float frameRate = format.getFrameRate() > 0.0F ? format.getFrameRate() : format.getSampleRate();
            if (frameRate <= 0.0F) return Optional.empty();

            long decodedBytes = 0L;
            while (true) {
                ByteBuffer data = stream.read(16_384);
                int bytes = data.remaining();
                if (bytes <= 0) break;
                decodedBytes += bytes;
            }
            return Optional.of(Math.max(0L, Math.round(decodedBytes * 1000.0D / (frameSize * frameRate))));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static InputStream open(Minecraft minecraft, Identifier location) throws IOException {
        Optional<java.nio.file.Path> direct = DirectSoundFiles.get(location);
        if (direct.isPresent()) return Files.newInputStream(direct.get());
        Optional<net.minecraft.server.packs.resources.Resource> resource = minecraft.getResourceManager().getResource(location);
        return resource.isPresent() ? resource.get().open() : null;
    }

    private static Optional<Long> readOggDuration(InputStream input) throws IOException {
        byte[] header = new byte[27];
        ByteArrayOutputStream firstPacket = new ByteArrayOutputStream(64);
        int sampleRate = 0;
        long lastGranule = -1L;

        while (readFully(input, header, 0, header.length)) {
            if (header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S') return Optional.empty();
            int segments = header[26] & 0xFF;
            byte[] segmentTable = input.readNBytes(segments);
            if (segmentTable.length != segments) return Optional.empty();
            int bodySize = 0;
            for (byte segment : segmentTable) bodySize += segment & 0xFF;
            byte[] body = input.readNBytes(bodySize);
            if (body.length != bodySize) return Optional.empty();

            if (firstPacket.size() < 64) {
                firstPacket.write(body, 0, Math.min(body.length, 64 - firstPacket.size()));
                sampleRate = sampleRate(firstPacket.toByteArray());
            }
            long granule = littleEndianLong(header, 6);
            if (granule >= 0L) lastGranule = granule;
        }

        if (sampleRate <= 0 || lastGranule < 0L) return Optional.empty();
        return Optional.of(Math.max(0L, lastGranule * 1000L / sampleRate));
    }

    private static int sampleRate(byte[] packet) {
        // Vorbis identification header: packet type, "vorbis", version, channels, rate.
        if (packet.length >= 16
                && packet[0] == 1
                && packet[1] == 'v' && packet[2] == 'o' && packet[3] == 'r'
                && packet[4] == 'b' && packet[5] == 'i' && packet[6] == 's') {
            return littleEndianInt(packet, 12);
        }
        // Opus specifies a fixed 48 kHz output rate; accepting it makes remote content future-safe.
        if (packet.length >= 19
                && packet[0] == 'O' && packet[1] == 'p' && packet[2] == 'u' && packet[3] == 's'
                && packet[4] == 'H' && packet[5] == 'e' && packet[6] == 'a' && packet[7] == 'd') {
            return 48_000;
        }
        return 0;
    }

    private static boolean readFully(InputStream input, byte[] target, int offset, int length) throws IOException {
        int total = 0;
        while (total < length) {
            int read = input.read(target, offset + total, length - total);
            // EOF means there is no next OGG page. Returning true here would
            // process the previous header again forever, which in turn left
            // the player timeline without a completed duration probe.
            if (read <= 0) return false;
            total += read;
        }
        return true;
    }

    private static int littleEndianInt(byte[] value, int offset) {
        return (value[offset] & 0xFF)
                | (value[offset + 1] & 0xFF) << 8
                | (value[offset + 2] & 0xFF) << 16
                | (value[offset + 3] & 0xFF) << 24;
    }

    private static long littleEndianLong(byte[] value, int offset) {
        long result = 0L;
        for (int i = 7; i >= 0; i--) {
            result = (result << 8) | (value[offset + i] & 0xFFL);
        }
        return result;
    }
}
