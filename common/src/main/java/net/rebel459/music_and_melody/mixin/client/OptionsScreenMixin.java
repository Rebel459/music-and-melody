package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.screen.AlbumScreen;
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

    @Inject(method = "init", at = @At("TAIL"))
    private void musicAndMelody$addAlbumScreenButton(CallbackInfo ci) {
        MaMConfig.ClientConfig.Albums albums = MaMConfig.get().client.albums;
        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.albums"), button ->
                this.minecraft.setScreen(new AlbumScreen(this))
        ).bounds(albums.position_x, albums.position_y, albums.size_x, albums.size_y).build());
    }
}
