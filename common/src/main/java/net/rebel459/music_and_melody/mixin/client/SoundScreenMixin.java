package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.screen.ContentBrowserScreen;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundScreenMixin extends Screen {

    protected SoundScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "addOptions", at = @At(value = "HEAD"))
    private void addButtonsToTop(CallbackInfo ci) {
        if (MaMClientConfig.get().button_placement == MaMClientConfig.ButtonPlacement.TOP) addButtons();
    }

    @Inject(method = "addOptions", at = @At(value = "TAIL"))
    private void addButtonsToBottom(CallbackInfo ci) {
        if (MaMClientConfig.get().button_placement == MaMClientConfig.ButtonPlacement.BOTTOM) addButtons();
    }

    @Unique
    private void addButtons() {
        SoundOptionsScreen screen = SoundOptionsScreen.class.cast(this);
        if (screen.list == null) return;

        Button albumsButton = Button.builder(Component.translatable("button.music_and_melody.albums"), button ->
                this.minecraft.setScreen(new ContentBrowserScreen(this))
        ).size(150, 20).build();

        Button playlistButton = Button.builder(Component.translatable("button.music_and_melody.playlist"), button ->
                this.minecraft.setScreen(new PlaylistScreen(this))
        ).size(150, 20).build();

        screen.list.addSmall(List.of(albumsButton, playlistButton));
    }
}
