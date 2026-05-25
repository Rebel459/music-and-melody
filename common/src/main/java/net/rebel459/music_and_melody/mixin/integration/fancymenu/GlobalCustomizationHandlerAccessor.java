package net.rebel459.music_and_melody.mixin.integration.fancymenu;

import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GlobalCustomizationHandler.class)
public interface GlobalCustomizationHandlerAccessor {

    @Accessor("currentMenuMusic")
    static IAudio getCurrentMenuMusic() {
        throw new AssertionError();
    }
}