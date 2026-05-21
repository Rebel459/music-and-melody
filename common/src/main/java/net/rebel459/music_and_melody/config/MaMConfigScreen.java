package net.rebel459.music_and_melody.config;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.screen.AlbumScreen;
import net.rebel459.music_and_melody.client.screen.EventScreen;
import net.rebel459.music_and_melody.client.screen.PlaylistScreen;

public class MaMConfigScreen extends Screen {
    private final Screen parent;

    public MaMConfigScreen(Screen parent) {
        super(Component.translatable("text.autoconfig.music_and_melody.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 68;

        this.addRenderableWidget(Button.builder(
                Component.translatable("text.autoconfig.music_and_melody/client.title"),
                button -> this.minecraft.setScreen(AutoConfigClient.getConfigScreen(MaMClientConfig.class, this).get())
        ).bounds(x, y, 200, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(
                Component.translatable("text.autoconfig.music_and_melody/server.title"),
                button -> this.minecraft.setScreen(AutoConfigClient.getConfigScreen(MaMServerConfig.class, this).get())
        ).bounds(x, y, 200, 20).build());

        y += 48;
        this.addRenderableWidget(Button.builder(
                Component.translatable("button.music_and_melody.albums"),
                button -> this.minecraft.setScreen(new AlbumScreen(this))
        ).bounds(x, y, 200, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(
                Component.translatable("button.music_and_melody.events"),
                button -> this.minecraft.setScreen(new EventScreen.EventSourceScreen(this))
        ).bounds(x, y, 200, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(
                Component.translatable("button.music_and_melody.playlist"),
                button -> this.minecraft.setScreen(new PlaylistScreen(this))
        ).bounds(x, y, 200, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(x, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
