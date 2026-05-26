package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.Playlist;

class SavePlaylistScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.save_playlist");
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final PlaylistScreen parent;
    private EditBox nameField;
    private EditBox iconField;
    private EditBox pathField;
    private Button saveButton;

    SavePlaylistScreen(PlaylistScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(300, this.width - 40);
        int fieldX = this.width / 2 - fieldWidth / 2;
        this.nameField = this.addRenderableWidget(new EditBox(this.font, fieldX, 62, fieldWidth, 20, Component.translatable("screen.music_and_melody.save_playlist.name")));
        this.nameField.setMaxLength(80);
        this.nameField.setResponder(value -> {
            updatePathHint();
            refreshSaveState();
        });
        this.iconField = this.addRenderableWidget(new EditBox(this.font, fieldX, 104, fieldWidth, 20, Component.translatable("screen.music_and_melody.save_playlist.icon")));
        this.iconField.setMaxLength(256);
        this.iconField.setResponder(value -> refreshSaveState());
        this.iconField.setHint(Component.literal(DEFAULT_ICON.toString()).withStyle(ChatFormatting.DARK_GRAY));
        this.pathField = this.addRenderableWidget(new EditBox(this.font, fieldX, 146, fieldWidth, 20, Component.translatable("screen.music_and_melody.save_playlist.path")));
        this.pathField.setMaxLength(256);
        this.pathField.setResponder(value -> refreshSaveState());
        updatePathHint();

        int buttonY = this.height - 27;
        int rowX = this.width / 2 - 154;
        this.saveButton = this.addRenderableWidget(Button.builder(saveMessage(), button -> save())
                .bounds(rowX, buttonY, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(rowX + 156, buttonY, 152, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
        this.setInitialFocus(this.nameField);
        refreshSaveState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        int fieldX = this.nameField.getX();
        graphics.text(this.font, Component.translatable("screen.music_and_melody.save_playlist.name"), fieldX, 50, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.save_playlist.icon"), fieldX, 92, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.save_playlist.path"), fieldX, 134, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void save() {
        if (Playlist.saveCurrentQueue(this.minecraft, this.nameField.getValue(), this.iconField.getValue(), this.pathField.getValue())) {
            this.onClose();
        }
    }

    private void refreshSaveState() {
        if (this.saveButton == null) return;
        this.saveButton.active = !this.nameField.getValue().trim().isEmpty()
                && iconValid()
                && Playlist.canWriteConfigPlaylist(this.nameField.getValue(), this.pathField.getValue());
        this.saveButton.setMessage(saveMessage());
    }

    private boolean iconValid() {
        String icon = this.iconField.getValue().trim();
        return icon.isEmpty() || Identifier.tryParse(icon) != null;
    }

    private Component saveMessage() {
        return Component.translatable(!this.nameField.getValue().trim().isEmpty()
                && Playlist.configPlaylistExists(this.nameField.getValue(), this.pathField == null ? "" : this.pathField.getValue())
                ? "button.music_and_melody.overwrite"
                : "button.music_and_melody.save");
    }

    private void updatePathHint() {
        if (this.pathField == null) return;
        String preview = Playlist.previewConfigPlaylistPath(this.nameField.getValue());
        this.pathField.setHint(preview.isEmpty() ? Component.empty() : Component.literal(preview).withStyle(ChatFormatting.DARK_GRAY));
    }
}
