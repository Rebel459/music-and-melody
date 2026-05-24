package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumManager;

import java.util.HashSet;
import java.util.Set;

public class RemoteAlbumDeleteScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.remote_downloads");

    private final Screen parent;
    private final Set<Identifier> pendingDeletes = new HashSet<>();
    private RemoteAlbumList list;

    public RemoteAlbumDeleteScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = this.addRenderableWidget(new RemoteAlbumList(this, this.minecraft, this.width, this.height - 64));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        boolean changed = false;

        for (Identifier id : Set.copyOf(this.pendingDeletes)) {
            changed |= RemoteAlbumManager.deleteInstalled(id);
        }

        this.pendingDeletes.clear();
        this.minecraft.setScreen(this.parent);

        if (changed) {
            this.minecraft.reloadResourcePacks();
        }
    }

    private boolean isDeletePending(RemoteAlbumManager.InstalledPack pack) {
        return this.pendingDeletes.contains(pack.id());
    }

    private void toggleDeletePending(RemoteAlbumManager.InstalledPack pack) {
        if (!this.pendingDeletes.remove(pack.id())) {
            this.pendingDeletes.add(pack.id());
        }

        refreshList();
    }

    private void refreshList() {
        if (this.list != null) this.list.refresh();
    }

    private static class RemoteAlbumList extends ObjectSelectionList<RemoteAlbumEntry> {

        private final RemoteAlbumDeleteScreen screen;

        RemoteAlbumList(RemoteAlbumDeleteScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 32, 32);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();

            RemoteAlbumManager.installedPacks().stream()
                    .map(pack -> new RemoteAlbumEntry(this.screen, this.minecraft, pack))
                    .forEach(this::addEntry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, this.width - 20);
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }
    }

    private static class RemoteAlbumEntry extends ObjectSelectionList.Entry<RemoteAlbumEntry> {

        private static final int BUTTON_GAP = 4;

        private final RemoteAlbumDeleteScreen screen;
        private final Minecraft minecraft;
        private final RemoteAlbumManager.InstalledPack pack;
        private final IconButton deleteButton;

        RemoteAlbumEntry(RemoteAlbumDeleteScreen screen, Minecraft minecraft, RemoteAlbumManager.InstalledPack pack) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.pack = pack;
            this.deleteButton = new IconButton(deleteMessage(), deleteIcon(), button -> {
                this.screen.toggleDeletePending(this.pack);
                ((IconButton) button).setIconAndTooltip(deleteIcon(), deleteMessage());
            });
        }

        @Override
        public Component getNarration() {
            return Component.empty()
                    .append(this.pack.name())
                    .append(CommonComponents.NARRATION_SEPARATOR)
                    .append(Component.literal(this.pack.id().toString()))
                    .append(CommonComponents.NARRATION_SEPARATOR)
                    .append(deleteMessage());
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int buttonsWidth = IconButton.SIZE;
            int maxTextWidth = Math.max(1, this.getContentWidth() - buttonsWidth - BUTTON_GAP - 12);
            int color = this.screen.isDeletePending(this.pack) ? 0xFFFF8888 : 0xFFFFFFFF;

            FormattedCharSequence name = this.minecraft.font
                    .split(this.pack.name(), maxTextWidth)
                    .getFirst();

            String id = this.minecraft.font.plainSubstrByWidth(this.pack.id().toString(), maxTextWidth);

            graphics.drawString(
                    this.minecraft.font,
                    name,
                    this.getContentX() + 1,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight - 1,
                    color
            );

            graphics.drawString(
                    this.minecraft.font,
                    Component.literal(id + " | " + this.pack.version()).withStyle(ChatFormatting.GRAY),
                    this.getContentX() + 1,
                    this.getContentYMiddle() + 2,
                    0xFFAAAAAA
            );

            this.deleteButton.setIconAndTooltip(deleteIcon(), deleteMessage());
            this.deleteButton.setX(this.getContentRight() - IconButton.SIZE);
            this.deleteButton.setY(this.getContentYMiddle() - 10);
            this.deleteButton.render(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.deleteButton.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
        }

        private Component deleteMessage() {
            return Component.translatable(this.screen.isDeletePending(this.pack)
                    ? "button.music_and_melody.restore"
                    : "button.music_and_melody.delete"
            );
        }

        private Identifier deleteIcon() {
            return IconButton.icon(this.screen.isDeletePending(this.pack) ? "restore" : "delete");
        }
    }
}