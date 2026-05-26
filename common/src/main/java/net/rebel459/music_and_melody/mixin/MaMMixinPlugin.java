package net.rebel459.music_and_melody.mixin;

import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class MaMMixinPlugin implements IMixinConfigPlugin {
    public static final boolean CURSEFORGE_DISTRIBUTION = hasClass("net.rebel459.music_and_melody.CurseForgeDistribution");

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    @Nullable
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.contains("integration.simple_music_control.")) return UnifiedPlatform.isModLoaded("simple_music_control");
        if (mixinClassName.contains("integration.enderscape.")) return UnifiedPlatform.isModLoaded("enderscape");
        if (mixinClassName.contains("integration.fancymenu.")) return UnifiedPlatform.isModLoaded("fancymenu");
        return true;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className, false, MaMMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    @Nullable
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
