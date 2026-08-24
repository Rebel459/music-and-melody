package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PauseScreen.class)
public abstract class PauseScreenMixin {

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/PauseScreen;getCustomAdditions()Ljava/util/Optional;"))
    private void addMusicButton(CallbackInfo ci, @Local(name = "iconButtonRow") LinearLayout iconButtonRow) {
        if (!MaMClientConfig.get().menu_buttons) return;
        PauseScreen screen = PauseScreen.class.cast(this);
        iconButtonRow.addChild(IconButton.createMusicPlayer(0, 0, _ -> screen.minecraft.gui.setScreen(MusicPlayerScreen.openLast(screen))));
    }
}