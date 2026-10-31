package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rebel459.music_and_melody.client.remote.DownloadedResources;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.util.DirectSoundFiles;
import net.rebel459.music_and_melody.client.util.MusicScreenHelper;
import net.rebel459.music_and_melody.client.util.VanillaSoundSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Unique
    private boolean clientAssets;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void invalidateDownloadedResources(PackType type, List<PackResources> packs, CallbackInfo ci) {
        this.clientAssets = type == PackType.CLIENT_RESOURCES;
        if (!this.clientAssets) return;
        DownloadedResources.invalidate();
        RemoteContentManager.markReloaded();
        DirectSoundFiles.clearResources();
        MusicScreenHelper.clearIconCache();
        VanillaSoundSupport.clear();
    }

    @Inject(method = "getNamespaces", at = @At("RETURN"), cancellable = true)
    private void addDownloadedNamespaces(CallbackInfoReturnable<Set<String>> cir) {
        if (!this.clientAssets) return;
        Set<String> namespaces = new HashSet<>(cir.getReturnValue());
        namespaces.addAll(DownloadedResources.namespaces());
        cir.setReturnValue(namespaces);
    }

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void getDownloadedResource(Identifier location, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (this.clientAssets && cir.getReturnValue().isEmpty()) {
            cir.setReturnValue(DownloadedResources.getResource(location));
        }
    }

    @Inject(method = "getResourceStack", at = @At("RETURN"), cancellable = true)
    private void getDownloadedResourceStack(Identifier location, CallbackInfoReturnable<List<Resource>> cir) {
        if (!this.clientAssets) return;
        List<Resource> resources = DownloadedResources.getResourceStack(location);
        if (resources.isEmpty()) return;
        List<Resource> stack = new ArrayList<>(cir.getReturnValue());
        stack.addAll(resources);
        cir.setReturnValue(stack);
    }

    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void listDownloadedResources(String directory, ResourceManager.Selector selector, CallbackInfoReturnable<Map<Identifier, Resource>> cir) {
        if (!this.clientAssets) return;
        Map<Identifier, Resource> resources = DownloadedResources.listResources(directory, selector);
        if (resources.isEmpty()) return;
        resources.putAll(cir.getReturnValue());
        cir.setReturnValue(resources);
    }
}
