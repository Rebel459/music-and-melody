package net.rebel459.legacies_and_legends.mixin.integration.enchants_and_expeditions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.rebel459.legacies_and_legends.registry.LaLToolMaterial;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LaLToolMaterial.class)
public class EaELaLToolMaterialMixin {

    @WrapOperation
            (
            method = "<clinit>",
            at = @At
                    (
                            value = "NEW",
                            target = "(Lnet/minecraft/tags/TagKey;IFFILnet/minecraft/tags/TagKey;)Lnet/minecraft/world/item/ToolMaterial;"
                    )
            )
    private static ToolMaterial modifyToolMaterial(TagKey tagKey, int i, float f, float g, int j, TagKey tagKey2, Operation<ToolMaterial> original){
        if (tagKey2 == LaLItemTags.FROSTED_TOOL_MATERIALS_FALLBACK) {
            return original.call(tagKey, i, f, g, j, LaLItemTags.FROSTED_TOOL_MATERIALS);
        } else {
            return original.call(tagKey, i, f, g, j, tagKey2);
        }
    }
}