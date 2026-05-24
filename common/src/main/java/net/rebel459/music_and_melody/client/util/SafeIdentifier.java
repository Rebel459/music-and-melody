package net.rebel459.music_and_melody.client.util;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SafeIdentifier {

    private final String namespace;
    private final String path;

    private SafeIdentifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static SafeIdentifier fromNamespaceAndPath(String namespace, String path) {
        return new SafeIdentifier(namespace, path);
    }

    public static SafeIdentifier withDefaultNamespace(String path) {
        return fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, path);
    }

    public static SafeIdentifier convert(ResourceLocation id) {
        return fromNamespaceAndPath(id.getNamespace(), id.getPath());
    }

    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(this.namespace, this.path);
    }

    @Override
    public @NotNull String toString() {
        return this.namespace + ":" + this.path;
    }

    public static SafeIdentifier parse(String string) {
        int separatorIndex = string.indexOf(":");
        if (separatorIndex >= 0) {
            String path = string.substring(separatorIndex + 1);
            if (separatorIndex != 0) {
                String namespace = string.substring(0, separatorIndex);
                return fromNamespaceAndPath(namespace, path);
            } else {
                return withDefaultNamespace(path);
            }
        } else {
            return withDefaultNamespace(string);
        }
    }

    public SafeIdentifier withPath(String newPath) {
        return fromNamespaceAndPath(this.namespace, newPath);
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SafeIdentifier other)) return false;
        return Objects.equals(this.namespace, other.namespace) && Objects.equals(this.path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.path);
    }
}
