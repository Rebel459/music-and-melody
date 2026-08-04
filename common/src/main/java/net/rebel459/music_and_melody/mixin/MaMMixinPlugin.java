package net.rebel459.music_and_melody.mixin;

import net.rebel459.unified.api.core.UnifiedInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class MaMMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    @Nullable
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.contains("integration.simple_music_control.")) return UnifiedInstance.isModLoaded("simple_music_control");
        if (mixinClassName.contains("integration.enderscape.")) return UnifiedInstance.isModLoaded("enderscape");
        if (mixinClassName.contains("integration.fancymenu.")) return UnifiedInstance.isModLoaded("fancymenu");
        return true;
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
