package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addMusicPlayer(CallbackInfo ci) {
        if (!MaMClientConfig.get().menu_buttons) return;

        TitleScreen screen = TitleScreen.class.cast(this);
        FriendsButton friends = screen.children().stream().filter(FriendsButton.class::isInstance).map(FriendsButton.class::cast).findFirst().orElse(null);
        if (friends == null) return;
        int rowY = friends.getY();
        List<AbstractWidget> buttons = screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getY() == rowY && widget.getWidth() == 20 && widget.getHeight() == 20)
                .sorted(Comparator.comparingInt(AbstractWidget::getX))
                .collect(Collectors.toCollection(ArrayList::new));

        IconButton musicButton = screen.addRenderableWidget(IconButton.createMusicPlayer(0, rowY, _ -> screen.minecraft.gui.setScreen(MusicPlayerScreen.openLast(screen))));

        buttons.add(musicButton);

        int buttonWidth = 20;
        int spacing = 4;
        int totalWidth = buttons.size() * buttonWidth + (buttons.size() - 1) * spacing;

        int startX = screen.width / 2 - totalWidth / 2;

        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setPosition(startX + i * (buttonWidth + spacing), rowY);
        }
    }
}
