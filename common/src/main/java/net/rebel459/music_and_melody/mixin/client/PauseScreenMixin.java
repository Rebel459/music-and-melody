package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    private static final int BUTTON_SPACING = 4;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addMusicPlayerButton(CallbackInfo ci) {
        MaMClientConfig.ButtonPosition position = MaMClientConfig.get().pause_button;
        if (position == MaMClientConfig.ButtonPosition.NONE) return;

        int buttonX;
        int buttonY;

        if (position == MaMClientConfig.ButtonPosition.REPLACE) {
            AbstractWidget feedback = findButton("menu.sendFeedback");
            AbstractWidget bugs = findButton("menu.reportBugs");
            if (feedback == null || bugs == null) return;

            int rowX = Math.min(feedback.getX(), bugs.getX());
            int rowRight = Math.max(feedback.getX() + feedback.getWidth(), bugs.getX() + bugs.getWidth());
            int sideWidth = (rowRight - rowX - IconButton.SIZE - BUTTON_SPACING * 2) / 2;
            if (sideWidth <= 0) return;

            buttonY = feedback.getY();
            feedback.setX(rowX);
            feedback.setWidth(sideWidth);
            buttonX = rowX + sideWidth + BUTTON_SPACING;
            bugs.setX(buttonX + IconButton.SIZE + BUTTON_SPACING);
            bugs.setWidth(rowRight - bugs.getX());
        } else {
            AbstractWidget statistics = findButton("gui.stats");
            if (statistics == null) return;

            buttonX = statistics.getX() + statistics.getWidth() + BUTTON_SPACING;
            buttonY = statistics.getY();
        }

        addRenderableWidget(IconButton.createMusicPlayer(buttonX, buttonY, _ -> minecraft.setScreen(MusicPlayerScreen.openLast(this))));
    }

    @Unique
    private AbstractWidget findButton(String translationKey) {
        Component message = Component.translatable(translationKey);
        return children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().equals(message))
                .findFirst()
                .orElse(null);
    }
}
