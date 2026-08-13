package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.client.remote.DownloadedResources;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void invalidateDownloadedResources(CallbackInfo ci) {
        DownloadedResources.invalidate();
    }

    @Inject(method = "getNamespaces", at = @At("RETURN"), cancellable = true)
    private void addDownloadedNamespaces(CallbackInfoReturnable<Set<String>> cir) {
        Set<String> namespaces = new HashSet<>(cir.getReturnValue());
        namespaces.addAll(DownloadedResources.namespaces());
        cir.setReturnValue(namespaces);
    }

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void getDownloadedResource(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (cir.getReturnValue().isEmpty()) {
            cir.setReturnValue(DownloadedResources.getResource(location));
        }
    }

    @Inject(method = "getResourceStack", at = @At("RETURN"), cancellable = true)
    private void getDownloadedResourceStack(ResourceLocation location, CallbackInfoReturnable<List<Resource>> cir) {
        List<Resource> resources = DownloadedResources.getResourceStack(location);
        if (resources.isEmpty()) return;
        List<Resource> stack = new ArrayList<>(cir.getReturnValue());
        stack.addAll(resources);
        cir.setReturnValue(stack);
    }

    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void listDownloadedResources(String directory, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> resources = DownloadedResources.listResources(directory, filter);
        if (resources.isEmpty()) return;
        resources.putAll(cir.getReturnValue());
        cir.setReturnValue(resources);
    }
}
