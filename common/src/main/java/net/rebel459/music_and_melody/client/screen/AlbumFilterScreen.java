package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumManager;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AlbumFilterScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.filter");
    private final AlbumScreen parent;

    public AlbumFilterScreen(AlbumScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        MaMDataConfig.Albums albums = MaMDataConfig.get().albums;
        int rowX = this.width / 2 - AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 2;
        int y = 48;
        addCheckbox("screen.music_and_melody.album_filter.favourites_only", rowX, y += 24, () -> albums.favourites_only, value -> albums.favourites_only = value);
        addCheckbox("screen.music_and_melody.album_filter.albums", rowX, y += 24, () -> albums.show_albums, value -> albums.show_albums = value);
        addCheckbox("screen.music_and_melody.album_filter.playlists", rowX, y += 24, () -> albums.show_playlists, value -> albums.show_playlists = value);
        addCheckbox("screen.music_and_melody.album_filter.remote", rowX, y += 24, () -> {
            return albums.show_remote;
        }, value -> {
            albums.show_remote = value;
            if (value) RemoteAlbumManager.refreshIfNeeded();
        });

        int buttonY = this.height - 27;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 4, buttonY, AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 2, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.parent.refreshList();
        this.minecraft.setScreen(this.parent);
    }

    private void addCheckbox(String key, int x, int y, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.addRenderableWidget(Checkbox.builder(Component.translatable(key), this.font)
                .pos(x, y)
                .selected(getter.get())
                .maxWidth(AlbumScreen.MAIN_BUTTON_ROW_WIDTH)
                .onValueChange((checkbox, selected) -> {
                    setter.accept(selected);
                    AutoConfig.getConfigHolder(MaMDataConfig.class).save();
                    this.parent.refreshList();
                })
                .build());
    }
}
