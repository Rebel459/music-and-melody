package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.screen.AlbumScreen;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;
import net.rebel459.music_and_melody.config.MaMConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )
    )
    private void musicAndMelody$addAlbumScreenButton(CallbackInfo ci, @Local(name = "helper") GridLayout.RowHelper helper) {
        Button albumsButton = Button.builder(Component.translatable("button.music_and_melody.albums"), button ->
                this.minecraft.setScreen(new AlbumScreen(this))
        ).size(150, 20).build();

        Button playlistButton = Button.builder(Component.translatable("button.music_and_melody.playlist"), button ->
                this.minecraft.setScreen(new PlaylistScreen(this))
        ).size(150, 20).build();

        if (MaMConfig.get().client.albums.button) helper.addChild(albumsButton);
        if (MaMConfig.get().client.playlist.button) helper.addChild(playlistButton);
    }
}
