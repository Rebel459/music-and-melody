package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addMusicPlayerButton(CallbackInfo ci) {
        MaMClientConfig.ButtonPosition position = MaMClientConfig.get().title_button;
        if (position == MaMClientConfig.ButtonPosition.NONE) return;

        AbstractWidget accessibility = children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().equals(Component.translatable("options.accessibility")))
                .findFirst()
                .orElse(null);
        if (accessibility == null) return;

        int buttonX = accessibility.getX();
        int buttonY = accessibility.getY();
        if (position == MaMClientConfig.ButtonPosition.REPLACE) {
            removeWidget(accessibility);
        } else {
            buttonX += accessibility.getWidth() + 4;
        }

        addRenderableWidget(IconButton.createMusicPlayer(buttonX, buttonY, _ -> minecraft.setScreen(MusicPlayerScreen.openLast(this))));
    }
}
