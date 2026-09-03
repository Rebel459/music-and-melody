package net.rebel459.music_and_melody.mixin.integration.fancymenu;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import net.minecraft.client.gui.screens.Screen;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = AbstractDecorationOverlay.class, remap = false)
public abstract class AbstractDecorationOverlayMixin {

    @Inject(method = "collectCollisionBoxes", at = @At("HEAD"), cancellable = true)
    private static void disableOverlayInteraction(@NotNull Screen screen, @NotNull List<AbstractElement> elements, CallbackInfoReturnable<List<CollisionAreaBounds>> cir) {
        if (ScreenIdentifierHandler.getIdentifierOfScreen(screen).contains(MusicAndMelody.MOD_ID)) cir.setReturnValue(List.of());
    }
}