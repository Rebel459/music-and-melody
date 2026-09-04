package net.rebel459.music_and_melody.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.SampledFloat;
import net.rebel459.music_and_melody.client.element.ExampleHintEditBox;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.util.CustomAlbums;
import net.rebel459.music_and_melody.client.util.CustomSounds;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.client.util.ThemeHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class SoundPoolsScreen extends Screen {

    private static final int HEADER_HEIGHT = 26;
    private static final int LABEL_HEIGHT = 15;
    private static final int SOUND_HEIGHT = 24;
    private static final int EXPANDED_FOOTER_HEIGHT = 26;
    private static final int HEADER_BUTTON_WIDTH = 70;
    private static final int HEADER_BUTTON_INSET = 6;

    private final Screen parent;
    private final List<PoolDraft> pools = new ArrayList<>();
    private final List<HeaderHit> headerHits = new ArrayList<>();
    private boolean loaded;
    private boolean loadingFields;
    private boolean dirty;
    private WorkspaceButton saveButton;
    private int expanded = -1;
    private double scroll;
    private double scrollMax;
    private int layoutWidth;
    private int layoutHeight;

    SoundPoolsScreen(Screen parent) {
        super(Component.translatable("screen.music_and_melody.sound_pools"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        calculateLayout();
        loadIfNeeded();
        updateScrollBounds();

        this.addRenderableOnly(this::renderEditor);
        this.addRenderableOnly((graphics, mouseX, mouseY, tickDelta) ->
                graphics.enableScissor(dialogX() + 2, contentTop(), dialogX() + dialogWidth() - 2, contentBottom()));

        this.loadingFields = true;
        buildVisibleWidgets();
        this.loadingFields = false;

        this.addRenderableOnly((graphics, mouseX, mouseY, tickDelta) -> graphics.disableScissor());
        buildFooter();
    }

    private void loadIfNeeded() {
        if (this.loaded) {
            return;
        }
        JsonObject root = CustomSounds.loadEditorJson();
        root.entrySet().forEach(entry -> this.pools.add(PoolDraft.read(entry.getKey(), entry.getValue())));
        this.loaded = true;
    }

    private void buildFooter() {
        int footerY = dialogY() + dialogHeight() - 28;
        int width = dialogWidth();
        int buttonWidth = (width - 32) / 3;
        int x = dialogX() + 12;

        this.addRenderableWidget(new WorkspaceButton(x, footerY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.new_pool"), false, ignored -> addPool()));
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(x + buttonWidth + 4, footerY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, ignored -> save()));
        this.saveButton.active = this.dirty && valid();
        this.addRenderableWidget(new WorkspaceButton(x + (buttonWidth + 4) * 2, footerY, buttonWidth, 20,
                CommonComponents.GUI_DONE, false, ignored -> done()));
    }

    private void buildVisibleWidgets() {
        clearTransientFields();
        int cursor = contentTop() - (int) Math.round(this.scroll);

        for (int poolIndex = 0; poolIndex < this.pools.size(); poolIndex++) {
            PoolDraft pool = this.pools.get(poolIndex);
            boolean open = poolIndex == this.expanded;

            if (fullyVisible(cursor, HEADER_HEIGHT)) {
                buildHeaderWidgets(pool, poolIndex, cursor, open);
            }
            cursor += HEADER_HEIGHT;

            if (!open) {
                continue;
            }

            cursor += LABEL_HEIGHT;
            for (int soundIndex = 0; soundIndex < pool.sounds.size(); soundIndex++) {
                SoundDraft sound = pool.sounds.get(soundIndex);
                if (fullyVisible(cursor, SOUND_HEIGHT)) {
                    buildSoundWidgets(pool, sound, soundIndex, cursor);
                }
                cursor += SOUND_HEIGHT;
            }

            if (fullyVisible(cursor, 20)) {
                this.addRenderableWidget(new WorkspaceButton(dialogX() + 12, cursor, 88, 20,
                        Component.translatable("button.music_and_melody.add_sound"), false,
                        ignored -> addSound(pool)));
            }
            cursor += EXPANDED_FOOTER_HEIGHT;
        }
    }

    private void clearTransientFields() {
        for (PoolDraft pool : this.pools) {
            for (SoundDraft sound : pool.sounds) {
                sound.volumeField = null;
            }
        }
    }

    private void buildHeaderWidgets(PoolDraft pool, int poolIndex, int y, boolean open) {
        int left = dialogX() + 12;
        int right = dialogX() + dialogWidth() - 12 - HEADER_BUTTON_INSET;
        int removeX = right - HEADER_BUTTON_WIDTH;
        int copyX = removeX - 4 - HEADER_BUTTON_WIDTH;

        if (open) {
            int fieldX = left + 22;
            int fieldWidth = Math.max(48, copyX - fieldX - 4);
            EditBox id = field(fieldX, y + 3, fieldWidth, "config:music", value -> {
                pool.id = stripNamespace(value);
                changed();
            });
            id.setValue("config:" + pool.id);
        }

        this.addRenderableWidget(new WorkspaceButton(copyX, y + 3, HEADER_BUTTON_WIDTH, 20,
                Component.translatable("button.music_and_melody.copy_id"), false,
                ignored -> this.minecraft.keyboardHandler.setClipboard("config:" + pool.id)));
        this.addRenderableWidget(new WorkspaceButton(removeX, y + 3, HEADER_BUTTON_WIDTH, 20,
                Component.translatable("button.music_and_melody.remove"), false,
                ignored -> removePool(poolIndex)));
    }

    private void buildSoundWidgets(PoolDraft pool, SoundDraft sound, int index, int y) {
        if (sound.automaticVolume && sound.volume.isBlank()) {
            applyDetectedVolume(sound);
        }
        int x = dialogX() + 12;
        int nameWidth = soundNameWidth();
        int typeX = x + nameWidth + 4;
        int weightX = typeX + 62;
        int volumeX = weightX + 58;
        int removeX = volumeX + 62;
        String nameExample = sound.type.equals("event") ? "minecraft:music.game" : "minecraft:music/game/sweden";
        EditBox name = field(x, y, nameWidth, nameExample, value -> {
            sound.name = value;
            if (sound.automaticVolume) {
                applyDetectedVolume(sound);
            }
            changed();
        });
        name.setValue(sound.name);

        this.addRenderableWidget(new WorkspaceButton(typeX, y, 58, 20,
                Component.literal(capitalizedType(sound.type)), false, ignored -> {
            sound.type = sound.type.equals("file") ? "event" : "file";
            sound.volume = "";
            sound.automaticVolume = true;
            if (sound.type.equals("file")) {
                applyDetectedVolume(sound);
            }
            changed();
            rebuildWidgets();
        }));

        EditBox weight = field(weightX, y, 54, "1", value -> {
            sound.weight = value;
            changed();
        });
        weight.setMaxLength(8);
        weight.setHint(sound.type.equals("file") ? defaultHint("1") : Component.empty());
        weight.setValue(sound.weight);

        EditBox volume = field(volumeX, y, 58, "1.0", value -> {
            sound.volume = value;
            sound.automaticVolume = value.isBlank();
            if (sound.automaticVolume) {
                applyDetectedVolume(sound);
            }
            changed();
        });
        sound.volumeField = volume;
        volume.setMaxLength(12);
        volume.setHint(sound.type.equals("file") ? defaultHint("1.0") : Component.empty());
        volume.setValue(sound.volume);
        volume.active = sound.type.equals("file");

        this.addRenderableWidget(new WorkspaceButton(removeX, y, 72, 20,
                Component.translatable("button.music_and_melody.remove"), false,
                ignored -> removeSound(pool, index)));
    }

    private EditBox field(int x, int y, int width, String example, Consumer<String> responder) {
        ExampleHintEditBox field = this.addRenderableWidget(
                new ExampleHintEditBox(this.font, x, y, width, 20, Component.empty()));
        field.setMaxLength(256);
        String hint = this.font.plainSubstrByWidth("eg. " + example, Math.max(0, width - 8));
        field.setHint(Component.literal(hint)
                .withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        field.setResponder(value -> {
            if (!this.loadingFields) {
                responder.accept(value);
            }
        });
        return field;
    }

    private Component defaultHint(String value) {
        return Component.literal(value).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE)));
    }

    private void applyDetectedVolume(SoundDraft sound) {
        String detected = detectedVolume(sound);
        sound.volume = detected;
        if (sound.volumeField == null || sound.volumeField.getValue().equals(detected)) {
            return;
        }

        boolean wasLoading = this.loadingFields;
        this.loadingFields = true;
        sound.volumeField.setValue(detected);
        this.loadingFields = wasLoading;
    }

    private String detectedVolume(SoundDraft sound) {
        if (!sound.type.equals("file") || sound.name.isBlank()) {
            return "";
        }

        SafeIdentifier id = CustomAlbums.playableId(SafeIdentifier.parse(sound.name.trim()));
        SampledFloat storedVolume = PlaylistHelper.STORED_VOLUME.get(id);
        if (storedVolume == null) {
            return "";
        }

        float volume = storedVolume.sample(SoundInstance.createUnseededRandom());
        return isDefaultVolume(volume) ? "" : Float.toString(volume);
    }

    private void renderEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = dialogX();
        int y = dialogY();
        int width = dialogWidth();
        int height = dialogHeight();

        if ((POPUP_OVERLAY >>> 24) != 0) {
            graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        }
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        outline(graphics, x, y, width, height);
        ThemeHelper.centeredText(graphics, this.font, this.title.copy().withStyle(ChatFormatting.BOLD),
                x + width / 2, y + 12, TEXT_TITLE);

        int top = contentTop();
        int bottom = contentBottom();
        graphics.enableScissor(x + 2, top, x + width - 2, bottom);
        renderPoolContents(graphics, mouseX, mouseY, x, width);
        graphics.disableScissor();

        if (this.scrollMax > 0) {
            renderScrollbar(graphics, x + width - 8, top, bottom, mouseX);
        }
    }

    private void renderPoolContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    int x, int width) {
        this.headerHits.clear();
        int cursor = contentTop() - (int) Math.round(this.scroll);

        for (int index = 0; index < this.pools.size(); index++) {
            PoolDraft pool = this.pools.get(index);
            boolean open = index == this.expanded;

            if (intersectsViewport(cursor, HEADER_HEIGHT)) {
                int headerLeft = x + 12;
                int headerRight = x + width - 12;
                boolean hover = mouseX >= headerLeft && mouseX < headerRight
                        && mouseY >= cursor && mouseY < cursor + HEADER_HEIGHT;
                if (hover) {
                    graphics.fill(headerLeft, cursor, headerRight, cursor + HEADER_HEIGHT, BUTTON_HIGHLIGHTED);
                }
                ThemeHelper.text(graphics, this.font, Component.literal(open ? "-" : "+"),
                        x + 18, cursor + 9, TEXT_PRIMARY);

                if (!open) {
                    int headerButtons = HEADER_BUTTON_WIDTH * 2 + 4;
                    int availableWidth = Math.max(0, width - headerButtons - 68);
                    String id = this.font.plainSubstrByWidth("config:" + pool.id, availableWidth);
                    ThemeHelper.text(graphics, this.font, Component.literal(id),
                            x + 38, cursor + 9, TEXT_PRIMARY);
                }
                this.headerHits.add(new HeaderHit(index, cursor));
            }
            cursor += HEADER_HEIGHT;

            if (!open) {
                continue;
            }

            if (intersectsViewport(cursor, LABEL_HEIGHT)) {
                int labelX = x + 12;
                ThemeHelper.text(graphics, this.font,
                        Component.translatable("screen.music_and_melody.sound_pools.name"),
                        labelX, cursor + 3, TEXT_DESCRIPTION);
                ThemeHelper.text(graphics, this.font,
                        Component.translatable("screen.music_and_melody.sound_pools.type_label"),
                        labelX + soundNameWidth() + 4, cursor + 3, TEXT_DESCRIPTION);
                ThemeHelper.text(graphics, this.font,
                        Component.translatable("screen.music_and_melody.sound_pools.weight"),
                        labelX + soundNameWidth() + 66, cursor + 3, TEXT_DESCRIPTION);
                ThemeHelper.text(graphics, this.font,
                        Component.translatable("screen.music_and_melody.sound_pools.volume"),
                        labelX + soundNameWidth() + 124, cursor + 3, TEXT_DESCRIPTION);
            }
            cursor += LABEL_HEIGHT + pool.sounds.size() * SOUND_HEIGHT + EXPANDED_FOOTER_HEIGHT;
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, int mouseX) {
        int viewport = bottom - top;
        int thumbHeight = Math.max(16, (int) Math.round(viewport * viewport / (viewport + this.scrollMax)));
        int thumbY = top + (int) Math.round((viewport - thumbHeight) * this.scroll / this.scrollMax);
        int color = mouseX >= x - 2 && mouseX <= x + 5 ? PANEL_HIGHLIGHTED : POPUP_OUTLINE;
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, color);
    }

    private int contentHeight() {
        int height = this.pools.size() * HEADER_HEIGHT;
        if (this.expanded >= 0 && this.expanded < this.pools.size()) {
            height += LABEL_HEIGHT;
            height += this.pools.get(this.expanded).sounds.size() * SOUND_HEIGHT;
            height += EXPANDED_FOOTER_HEIGHT;
        }
        return height;
    }

    private void updateScrollBounds() {
        this.scrollMax = Math.max(0, contentHeight() - (contentBottom() - contentTop()));
        this.scroll = Math.max(0, Math.min(this.scroll, this.scrollMax));
    }

    private void addPool() {
        String id = "custom_pool";
        int suffix = 2;
        while (containsPool(id)) {
            id = "custom_pool_" + suffix++;
        }

        PoolDraft pool = new PoolDraft(id, new JsonObject(), new ArrayList<>());
        this.pools.add(pool);
        this.expanded = this.pools.size() - 1;
        this.scroll = Double.MAX_VALUE;
        changed();
        rebuildWidgets();
    }

    private void removePool(int index) {
        this.pools.remove(index);
        if (this.expanded == index) {
            this.expanded = -1;
        } else if (this.expanded > index) {
            this.expanded--;
        }
        changed();
        rebuildWidgets();
    }

    private boolean containsPool(String id) {
        return this.pools.stream().anyMatch(pool -> pool.id.equals(id));
    }

    private void addSound(PoolDraft pool) {
        pool.sounds.add(SoundDraft.create());
        changed();
        rebuildWidgets();
    }

    private void removeSound(PoolDraft pool, int index) {
        pool.sounds.remove(index);
        changed();
        rebuildWidgets();
    }

    private void changed() {
        this.dirty = true;
        if (this.saveButton != null) {
            this.saveButton.active = valid();
        }
    }

    private boolean save() {
        if (!valid()) {
            return false;
        }

        JsonObject root = editorJson();
        if (!CustomSounds.saveEditorJson(root)) {
            return false;
        }

        this.dirty = false;
        this.minecraft.reloadResourcePacks();
        rebuildWidgets();
        return true;
    }

    private JsonObject editorJson() {
        JsonObject root = new JsonObject();
        this.pools.forEach(pool -> root.add(pool.id, pool.write()));
        return root;
    }

    private boolean valid() {
        Set<String> ids = new HashSet<>();
        for (PoolDraft pool : this.pools) {
            if (pool.id.isBlank() || Identifier.tryBuild("config", pool.id) == null || !ids.add(pool.id)) {
                return false;
            }
            for (SoundDraft sound : pool.sounds) {
                if (sound.name.isBlank() || !validWeight(sound.weight) || !validVolume(sound.volume)) {
                    return false;
                }
                if (sound.type.equals("event") && Identifier.tryParse(sound.name) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void done() {
        if (this.dirty) {
            this.minecraft.gui.setScreen(new ThemeExitConfirmScreen(this,
                    Component.translatable("screen.music_and_melody.sound_pools.unsaved"),
                    Component.translatable("screen.music_and_melody.sound_pools.unsaved_warning"), this::finish));
        } else {
            finish(false);
        }
    }

    void finish(boolean save) {
        if (!save || save()) {
            this.minecraft.gui.setScreen(this.parent);
        } else {
            this.minecraft.gui.setScreen(this);
        }
    }

    @Override
    public void onClose() {
        done();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, -1, -1, tickDelta);
        IconButton.setTooltipScale(MaMDataConfig.get().gui_multiplier);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
            super.extractRenderState(graphics, scaled(mouseX), scaled(mouseY), tickDelta);
        } finally {
            graphics.pose().popMatrix();
            IconButton.resetTooltipScale();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        MouseButtonEvent scaled = new MouseButtonEvent(
                event.x() / MaMDataConfig.get().gui_multiplier,
                event.y() / MaMDataConfig.get().gui_multiplier,
                event.buttonInfo());
        if (super.mouseClicked(scaled, doubleClick)) {
            return true;
        }

        for (HeaderHit hit : this.headerHits) {
            if (scaled.x() >= dialogX() + 12
                    && scaled.x() < dialogX() + dialogWidth() - 12
                    && scaled.y() >= hit.y
                    && scaled.y() < hit.y + HEADER_HEIGHT) {
                this.expanded = this.expanded == hit.index ? -1 : hit.index;
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = scaled(mouseX);
        int y = scaled(mouseY);
        if (x >= dialogX() && x < dialogX() + dialogWidth()
                && y >= contentTop() && y < contentBottom()) {
            this.scroll = Math.max(0, Math.min(this.scrollMax, this.scroll - scrollY * 24));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void repositionElements() {
        calculateLayout();
        rebuildWidgets();
    }

    private boolean fullyVisible(int y, int height) {
        return y >= contentTop() && y + height <= contentBottom();
    }

    private int soundNameWidth() {
        int availableWidth = dialogWidth() - 24 - HEADER_BUTTON_INSET;
        return Math.max(48, Math.min(246, availableWidth - 258));
    }

    private boolean intersectsViewport(int y, int height) {
        return y + height >= contentTop() && y <= contentBottom();
    }

    private int contentTop() {
        return dialogY() + 31;
    }

    private int contentBottom() {
        return dialogY() + dialogHeight() - 35;
    }

    private int dialogWidth() {
        return Math.max(1, Math.min(620, this.layoutWidth - 24));
    }

    private int dialogHeight() {
        return Math.max(1, this.layoutHeight - 24);
    }

    private int dialogX() {
        return this.layoutWidth / 2 - dialogWidth() / 2;
    }

    private int dialogY() {
        return this.layoutHeight / 2 - dialogHeight() / 2;
    }

    private int scaled(double value) {
        return Math.round((float) (value / MaMDataConfig.get().gui_multiplier));
    }

    private void calculateLayout() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
    }

    private static String stripNamespace(String id) {
        String trimmed = id.trim();
        return trimmed.startsWith("config:") ? trimmed.substring(7) : trimmed;
    }

    private static String capitalizedType(String type) {
        return type.equals("event") ? "Event" : "File";
    }

    private static boolean validWeight(String value) {
        if (value.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validVolume(String value) {
        if (value.isBlank()) {
            return true;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            return parsed > 0 && Float.isFinite(parsed);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isDefaultVolume(float volume) {
        return Float.compare(volume, 1.0F) == 0;
    }

    private record HeaderHit(int index, int y) {
    }

    private static final class PoolDraft {

        private String id;
        private final JsonObject source;
        private final List<SoundDraft> sounds;

        private PoolDraft(String id, JsonObject source, List<SoundDraft> sounds) {
            this.id = id;
            this.source = source;
            this.sounds = sounds;
        }

        private static PoolDraft read(String id, JsonElement value) {
            JsonObject source = value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
            List<SoundDraft> sounds = new ArrayList<>();
            JsonArray array = source.getAsJsonArray("sounds");
            if (array != null) {
                array.forEach(sound -> sounds.add(SoundDraft.read(sound)));
            }
            return new PoolDraft(id, source, sounds);
        }

        private JsonObject write() {
            JsonObject result = this.source.deepCopy();
            JsonArray array = new JsonArray();
            this.sounds.forEach(sound -> array.add(sound.write()));
            result.add("sounds", array);
            return result;
        }

    }

    private static final class SoundDraft {

        private final JsonObject source;
        private String name;
        private String type;
        private String weight;
        private String volume;
        private boolean automaticVolume;
        private EditBox volumeField;

        private SoundDraft(JsonObject source, String name, String type, String weight,
                           String volume, boolean automaticVolume) {
            this.source = source;
            this.name = name;
            this.type = type;
            this.weight = weight;
            this.volume = volume;
            this.automaticVolume = automaticVolume;
        }

        private static SoundDraft create() {
            return new SoundDraft(new JsonObject(), "", "file", "", "", true);
        }

        private static SoundDraft read(JsonElement value) {
            if (value.isJsonPrimitive()) {
                return new SoundDraft(new JsonObject(), value.getAsString(), "file", "", "", true);
            }

            JsonObject source = value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
            String name = source.has("name") ? source.get("name").getAsString() : "";
            String type = source.has("type") && source.get("type").getAsString().equals("event")
                    ? "event"
                    : "file";
            String weight = type.equals("event")
                    ? source.has("weight") ? source.get("weight").getAsString() : ""
                    : defaultedNumber(source, "weight", 1.0F);
            String volume = defaultedNumber(source, "volume", 1.0F);
            return new SoundDraft(source, name, type, weight, volume, volume.isBlank());
        }

        private JsonElement write() {
            JsonObject result = this.source.deepCopy();
            result.addProperty("name", this.name);
            result.addProperty("type", this.type);

            result.remove("weight");
            if (!this.weight.isBlank()) {
                int parsedWeight = Integer.parseInt(this.weight.trim());
                if (this.type.equals("event") || parsedWeight != 1) {
                    result.addProperty("weight", parsedWeight);
                }
            }

            result.remove("volume");
            if (!this.volume.isBlank()) {
                float parsedVolume = Float.parseFloat(this.volume.trim());
                if (!isDefaultVolume(parsedVolume)) {
                    result.addProperty("volume", parsedVolume);
                }
            }

            result.addProperty("stream", true);
            return result;
        }

        private static String defaultedNumber(JsonObject source, String property, float defaultValue) {
            if (!source.has(property)) {
                return "";
            }
            String value = source.get(property).getAsString();
            try {
                return Float.compare(Float.parseFloat(value), defaultValue) == 0 ? "" : value;
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
    }
}
