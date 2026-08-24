package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.util.CustomAlbums;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class AlbumEditorScreen extends Screen {

    private final MusicPlayerScreen parent;
    private final Identifier albumId;
    private final List<String> existingTracks;
    private final List<Path> addedTracks = new ArrayList<>();
    private final Set<String> removedTracks = new HashSet<>();
    private final Set<Path> removedAddedTracks = new HashSet<>();
    private EditBox nameField;
    private EditBox iconField;
    private EditBox identifierField;
    private WorkspaceButton saveButton;
    private TrackList trackList;
    private int layoutWidth;
    private int layoutHeight;

    AlbumEditorScreen(MusicPlayerScreen parent) {
        this(parent, null);
    }

    AlbumEditorScreen(MusicPlayerScreen parent, Identifier albumId) {
        super(Component.translatable(albumId == null ? "button.music_and_melody.new_album" : "screen.music_and_melody.edit_album"));
        this.parent = parent;
        this.albumId = albumId;
        this.existingTracks = albumId == null ? List.of() : CustomAlbums.trackFiles(albumId);
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int x = panelX();
        int y = panelY();
        int fieldX = x + 12;
        int fieldWidth = panelWidth() - 24;
        CustomAlbums.Metadata metadata = this.albumId == null ? null : CustomAlbums.metadata(this.albumId).orElse(null);

        this.nameField = field(Component.translatable("screen.music_and_melody.name"), fieldX, y + 42, fieldWidth);
        this.nameField.setMaxLength(80);
        this.nameField.setValue(metadata == null ? "" : metadata.name().getString());
        this.nameField.setResponder(value -> {
            updateIdentifierHint();
            refreshSaveState();
        });

        this.iconField = field(Component.translatable("screen.music_and_melody.icon"), fieldX, y + 78, fieldWidth);
        this.iconField.setHint(Component.literal("minecraft:textures/misc/unknown_pack.png").withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        this.iconField.setValue(metadata == null ? "" : metadata.icon().toString());
        this.iconField.setResponder(value -> refreshSaveState());

        this.identifierField = field(Component.translatable("screen.music_and_melody.path"), fieldX, y + 114, fieldWidth);
        this.identifierField.setValue(this.albumId == null ? "" : this.albumId.getPath());
        this.identifierField.setEditable(this.albumId == null);
        this.identifierField.setResponder(value -> refreshSaveState());
        updateIdentifierHint();

        int selectorY = y + 150;
        this.addRenderableWidget(new WorkspaceButton(fieldX, selectorY, fieldWidth, 20,
                Component.translatable("screen.music_and_melody.album_import_music"), false, ignored -> selectMusic()));

        this.trackList = this.addRenderableWidget(new TrackList(this.minecraft, fieldX, fieldWidth, selectorY + 26, y + panelHeight() - 34));
        int buttonY = y + panelHeight() - 29;
        int buttonWidth = (fieldWidth - 5) / 2;
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(fieldX, buttonY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, ignored -> save()));
        this.addRenderableWidget(new WorkspaceButton(fieldX + buttonWidth + 5, buttonY, buttonWidth, 20,
                CommonComponents.GUI_CANCEL, false, ignored -> this.onClose()));
        this.setInitialFocus(this.nameField);
        refreshSaveState();
    }

    private EditBox field(Component message, int x, int y, int width) {
        EditBox field = this.addRenderableWidget(new EditBox(this.font, x, y, width, 20, message));
        field.setMaxLength(256);
        return field;
    }

    private void selectMusic() {
        CustomAlbums.chooseAudioFiles(files -> {
            this.addedTracks.addAll(files);
            refreshTrackList();
            refreshSaveState();
        });
    }

    private void save() {
        List<Path> keptAddedTracks = this.addedTracks.stream().filter(track -> !this.removedAddedTracks.contains(track)).toList();
        boolean saved = this.albumId == null
                ? CustomAlbums.create(this.nameField.getValue(), this.iconField.getValue(), this.identifierField.getValue(), keptAddedTracks)
                : CustomAlbums.update(this.albumId, this.nameField.getValue(), this.iconField.getValue(), keptAddedTracks, this.removedTracks);
        if (saved) finish();
    }

    private void finish() {
        this.parent.configAlbumsChanged();
        this.minecraft.setScreen(this.parent);
    }

    private void refreshTrackList() {
        if (this.trackList != null) this.trackList.refresh();
    }

    private void refreshSaveState() {
        if (this.saveButton == null) return;
        boolean hasTracks = this.addedTracks.stream().anyMatch(track -> !this.removedAddedTracks.contains(track))
                || this.existingTracks.stream().anyMatch(track -> !this.removedTracks.contains(track));
        boolean identifierValid = this.albumId != null || CustomAlbums.canUseIdentifier(this.identifierField.getValue().isBlank() ? this.nameField.getValue() : this.identifierField.getValue());
        this.saveButton.active = !this.nameField.getValue().trim().isEmpty() && hasTracks && identifierValid && validIcon();
    }

    private boolean validIcon() {
        return CustomAlbums.validIconInput(this.iconField.getValue());
    }

    private void updateIdentifierHint() {
        if (this.identifierField == null || this.albumId != null) return;
        String preview = CustomAlbums.previewIdentifier(this.nameField.getValue());
        this.identifierField.setHint(preview.isBlank() ? Component.empty() : Component.literal(preview).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, -1, -1, tickDelta);
        IconButton.setTooltipScale(MaMDataConfig.get().gui_multiplier);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
            super.extractRenderState(graphics, toLayoutMouse(mouseX), toLayoutMouse(mouseY), tickDelta);
        } finally {
            graphics.pose().popMatrix();
            IconButton.resetTooltipScale();
        }
    }

    @Override
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        event = toLayoutMouse(event);
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        event = toLayoutMouse(event);
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseDragged(event, dragX / MaMDataConfig.get().gui_multiplier, dragY / MaMDataConfig.get().gui_multiplier);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = toLayoutMouse(event);
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseReleased(event);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        centeredText(graphics, this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
        text(graphics, this.font, Component.translatable("screen.music_and_melody.name"), x + 12, y + 30, TEXT_DESCRIPTION);
        text(graphics, this.font, Component.translatable("screen.music_and_melody.icon"), x + 12, y + 66, TEXT_DESCRIPTION);
        text(graphics, this.font, Component.translatable("screen.music_and_melody.path"), x + 12, y + 102, TEXT_DESCRIPTION);
        text(graphics, this.font, Component.translatable("screen.music_and_melody.album_tracks", trackCount()), x + 12, y + 138, TEXT_DESCRIPTION);
    }

    private int trackCount() {
        return (int) (this.existingTracks.stream().filter(track -> !this.removedTracks.contains(track)).count()
                + this.addedTracks.stream().filter(track -> !this.removedAddedTracks.contains(track)).count());
    }

    private void toggleExisting(String path) {
        if (!this.removedTracks.add(path)) this.removedTracks.remove(path);
        refreshSaveState();
    }

    private void toggleAdded(Path path) {
        if (!this.removedAddedTracks.add(path)) this.removedAddedTracks.remove(path);
        refreshSaveState();
    }

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    private int toLayoutMouse(int coordinate) {
        return Math.round(coordinate / MaMDataConfig.get().gui_multiplier);
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int panelWidth() {
        return Math.min(420, this.layoutWidth - 24);
    }

    private int panelHeight() {
        return Math.min(360, this.layoutHeight - 28);
    }

    private int panelX() {
        return this.layoutWidth / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.layoutHeight / 2 - panelHeight() / 2;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return mouseX >= panelX() && mouseX < panelX() + panelWidth() && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    private final class TrackList extends ObjectSelectionList<TrackEntry> {
        private final int panelX;
        private final int panelWidth;

        TrackList(Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, 21);
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
            this.centerListVertically = false;
            refresh();
        }

        void refresh() {
            double scroll = this.scrollAmount();
            this.clearEntries();
            existingTracks.forEach(path -> this.addEntry(new TrackEntry(path, null)));
            addedTracks.forEach(path -> this.addEntry(new TrackEntry(null, path)));
            this.setScrollAmount(scroll);
        }

        @Override protected void extractListBackground(GuiGraphicsExtractor graphics) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
        @Override public int getRowLeft() { return this.panelX; }
        @Override public int getRowWidth() { return this.panelWidth - 7; }
        @Override protected int scrollBarX() { return this.panelX + this.panelWidth - 4; }
    }

    private final class TrackEntry extends ObjectSelectionList.Entry<TrackEntry> {
        private final String existing;
        private final Path added;

        private TrackEntry(String existing, Path added) {
            this.existing = existing;
            this.added = added;
        }

        @Override public Component getNarration() { return Component.literal(label()); }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean removing = this.existing != null ? removedTracks.contains(this.existing) : removedAddedTracks.contains(this.added);
            if (hovered || removing) graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), removing ? BUTTON_DISABLED : BUTTON_HIGHLIGHT);
            String prefix = removing ? "\u00D7 " : "\u2022 ";
            int colour = removing ? TEXT_DISABLED : TEXT_PRIMARY;
            text(graphics, font, Component.literal(prefix + label()), getContentX() + 3, getContentYMiddle() - font.lineHeight / 2, colour);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.existing != null) toggleExisting(this.existing);
            else toggleAdded(this.added);
            return true;
        }

        private String label() {
            return this.existing != null ? this.existing : this.added.getFileName().toString();
        }
    }
}
