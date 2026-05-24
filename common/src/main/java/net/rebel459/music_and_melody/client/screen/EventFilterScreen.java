package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EventFilterScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.filter");
    private final EventScreen.EventSourceScreen parent;

    public EventFilterScreen(EventScreen.EventSourceScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        MaMDataConfig.Events events = MaMDataConfig.get().events;
        int rowX = this.width / 2 - AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 2;
        int y = 48;

        addCheckbox("screen.music_and_melody.filter.inclusive", rowX, y, () -> events.filter_inclusive, value -> events.filter_inclusive = value);
        addCheckbox("screen.music_and_melody.event_filter.custom", rowX, y += 24, () -> events.filter_custom, value -> events.filter_custom = value);
        addCheckbox("screen.music_and_melody.event_filter.built_in", rowX, y += 24, () -> events.filter_built_in, value -> events.filter_built_in = value);
        addCheckbox("screen.music_and_melody.event_filter.enabled", rowX, y += 24, () -> events.filter_enabled, value -> events.filter_enabled = value);
        addCheckbox("screen.music_and_melody.event_filter.disabled", rowX, y += 24, () -> events.filter_disabled, value -> events.filter_disabled = value);

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