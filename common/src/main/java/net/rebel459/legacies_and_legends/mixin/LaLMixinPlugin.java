package net.rebel459.legacies_and_legends.mixin;

import net.rebel459.unified.platform.UnifiedPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class LaLMixinPlugin implements IMixinConfigPlugin {

    public static boolean hasEnchantsAndExpeditions;
    public static boolean hasCombatReborn;
    public static boolean hasFriendsAndFoes;

    @Override
    public void onLoad(String mixinPackage) {
        hasEnchantsAndExpeditions = UnifiedPlatform.isModLoaded("enchants_and_expeditions");
        hasCombatReborn = UnifiedPlatform.isModLoaded("combat_reborn");
        hasFriendsAndFoes = UnifiedPlatform.isModLoaded("friendsandfoes");
    }

    @Override
    @Nullable
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.contains("integration.enchants_and_expeditions.")) return hasEnchantsAndExpeditions;
        if (mixinClassName.contains("integration.combat_reborn.")) return hasCombatReborn;
        if (mixinClassName.contains("integration.friendsandfoes.")) return hasFriendsAndFoes;

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
