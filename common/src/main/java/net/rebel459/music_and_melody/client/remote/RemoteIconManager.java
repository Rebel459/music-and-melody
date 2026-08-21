package net.rebel459.music_and_melody.client.remote;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

public final class RemoteIconManager {

    public static final Identifier FALLBACK = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Map<String, Identifier> LOADED = new HashMap<>();
    private static final Set<String> REQUESTED = new HashSet<>();
    private static final Set<Identifier> DYNAMIC_TEXTURES = new HashSet<>();
    private static final Map<String, Image> IMAGES = new HashMap<>();
    private static final Set<String> REQUESTED_IMAGES = new HashSet<>();

    private RemoteIconManager() {}

    public static synchronized Identifier icon(RemotePack pack) {
        if (!RemoteContentManager.onlineFunctionalityEnabled()) return FALLBACK;
        String value = pack.icon();
        Identifier identifier = Identifier.tryParse(value);
        if (identifier != null) return identifier;

        URI uri = imageUri(value);
        if (uri == null) return FALLBACK;

        Identifier loaded = LOADED.get(value);
        if (loaded != null) return loaded;
        if (!REQUESTED.add(value)) return FALLBACK;

        RemoteContentManager.loadIcon(uri).thenAccept(bytes -> {
            if (bytes == null) return;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> register(minecraft, value, bytes));
        });
        return FALLBACK;
    }

    public static synchronized boolean isDynamic(Identifier icon) {
        return DYNAMIC_TEXTURES.contains(icon);
    }

    /** Requests a remote Markdown image and returns it once it is ready to render. */
    public static synchronized Image image(String value) {
        if (!RemoteContentManager.onlineFunctionalityEnabled()) return null;
        Image loaded = IMAGES.get(value);
        if (loaded != null) return loaded;
        URI uri = imageUri(value);
        if (uri == null || !REQUESTED_IMAGES.add(value)) return null;
        RemoteContentManager.loadIcon(uri).thenAccept(bytes -> {
            if (bytes == null) return;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> registerImage(minecraft, value, bytes));
        });
        return null;
    }

    private static void register(Minecraft minecraft, String url, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(bytes);
            Identifier id = Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "remote_icons/" + hash(url));
            minecraft.getTextureManager().register(id, new DynamicTexture(() -> "Remote icon " + url, image));
            synchronized (RemoteIconManager.class) {
                LOADED.put(url, id);
                DYNAMIC_TEXTURES.add(id);
            }
        } catch (IOException ignored) {
        }
    }

    private static void registerImage(Minecraft minecraft, String url, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(bytes);
            Identifier id = Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "remote_images/" + hash(url));
            int width = image.getWidth();
            int height = image.getHeight();
            minecraft.getTextureManager().register(id, new DynamicTexture(() -> "Remote image " + url, image));
            synchronized (RemoteIconManager.class) {
                IMAGES.put(url, new Image(id, width, height));
                DYNAMIC_TEXTURES.add(id);
            }
        } catch (IOException ignored) {
        }
    }

    private static URI imageUri(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) return null;
            return RemoteContentManager.rawGithubBlobUri(uri);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record Image(Identifier texture, int width, int height) {}

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
