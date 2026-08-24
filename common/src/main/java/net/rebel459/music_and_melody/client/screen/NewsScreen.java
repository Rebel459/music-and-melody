package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;
import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class NewsScreen extends Screen {

    private static final String NEWS_URL = "https://raw.githubusercontent.com/Rebel459/music-and-melody-remote/main/news.md";
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final MusicPlayerScreen parent;
    private final List<Block> blocks = new ArrayList<>();
    private String error;
    private boolean loading = true;
    private double scroll;
    private double scrollMax;
    private int layoutWidth;
    private int layoutHeight;

    NewsScreen(MusicPlayerScreen parent) {
        super(Component.translatable("button.music_and_melody.news"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int width = dialogWidth();
        this.addRenderableWidget(new WorkspaceButton(dialogX() + width - 72, dialogY() + dialogHeight() - 28, 60, 20,
                CommonComponents.GUI_DONE, false, ignored -> onClose()));
        load();
    }

    private void load() {
        if (!RemoteContentManager.onlineFunctionalityEnabled()) {
            this.loading = false;
            this.error = "Online functionality is disabled";
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(NEWS_URL)).timeout(Duration.ofSeconds(20)).GET().build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
                return response.body();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((markdown, exception) -> this.minecraft.execute(() -> {
            this.loading = false;
            if (exception != null) this.error = exception.getCause() == null ? exception.getMessage() : exception.getCause().getMessage();
            else parse(markdown);
        }));
    }

    private void parse(String markdown) {
        this.blocks.clear();
        URI base = URI.create(NEWS_URL);
        for (String raw : markdown.replace("\r", "").split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                this.blocks.add(new TextBlock("", Kind.SPACER));
            } else if (line.matches("^!\\[[^]]*]\\([^)]+\\)$")) {
                int start = line.indexOf(']') + 2;
                this.blocks.add(new ImageBlock(base.resolve(line.substring(start, line.length() - 1)).toString()));
            } else if (line.startsWith("### ")) {
                this.blocks.add(new TextBlock(line.substring(4), Kind.HEADING_SMALL));
            } else if (line.startsWith("## ")) {
                this.blocks.add(new TextBlock(line.substring(3), Kind.HEADING_MEDIUM));
            } else if (line.startsWith("# ")) {
                this.blocks.add(new TextBlock(line.substring(2), Kind.HEADING_LARGE));
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                this.blocks.add(new TextBlock("Ã¢â‚¬Â¢ " + line.substring(2), Kind.BODY));
            } else if (line.startsWith("> ")) {
                this.blocks.add(new TextBlock(line.substring(2), Kind.QUOTE));
            } else if (line.equals("---") || line.equals("***")) {
                this.blocks.add(new TextBlock("", Kind.RULE));
            } else {
                this.blocks.add(new TextBlock(line, Kind.BODY));
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

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

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = dialogX(), y = dialogY(), width = dialogWidth(), height = dialogHeight();
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        ThemeHelper.centeredText(graphics, this.font, this.title, x + width / 2, y + 12, TEXT_TITLE);

        int top = y + 31, bottom = y + height - 35;
        if (this.loading) {
            ThemeHelper.centeredText(graphics, this.font, Component.translatable("screen.music_and_melody.loading"), x + width / 2, top + 8, TEXT_DESCRIPTION);
            return;
        }
        if (this.error != null) {
            ThemeHelper.centeredText(graphics, this.font, Component.literal("Unable to load news: " + this.error), x + width / 2, top + 8, TEXT_HEADER_SECONDARY);
            return;
        }
        int contentWidth = width - 32;
        this.scrollMax = Math.max(0, contentHeight(contentWidth) - (bottom - top));
        this.scroll = Math.max(0, Math.min(this.scroll, this.scrollMax));
        int cursor = top - (int) Math.round(this.scroll);
        for (Block block : this.blocks) {
            int blockHeight = height(block, contentWidth);
            if (cursor + blockHeight >= top && cursor <= bottom) renderBlock(graphics, block, x + 16, cursor, contentWidth, top, bottom);
            cursor += blockHeight;
        }
        if (this.scrollMax > 0) renderScrollbar(graphics, x + width - 8, top, bottom, mouseX, mouseY);
    }

    private int contentHeight(int width) {
        int result = 0;
        for (Block block : this.blocks) result += height(block, width);
        return result;
    }

    private int height(Block block, int width) {
        if (block instanceof ImageBlock image) {
            RemoteIconManager.Image loaded = RemoteIconManager.image(image.url());
            return loaded == null ? 82 : Math.min(260, Math.max(32, width * loaded.height() / Math.max(1, loaded.width()))) + 8;
        }
        TextBlock text = (TextBlock) block;
        return switch (text.kind()) {
            case SPACER -> 7;
            case RULE -> 9;
            case HEADING_LARGE -> this.font.lineHeight + 9;
            case HEADING_MEDIUM -> this.font.lineHeight + 7;
            case HEADING_SMALL -> this.font.lineHeight + 5;
            default -> this.font.split(inline(text.value()), width).size() * (this.font.lineHeight + 2) + 3;
        };
    }

    private void renderBlock(GuiGraphicsExtractor graphics, Block block, int x, int y, int width, int top, int bottom) {
        if (block instanceof ImageBlock image) {
            RemoteIconManager.Image loaded = RemoteIconManager.image(image.url());
            int imageHeight = height(image, width) - 8;
            if (loaded == null) {
                graphics.fill(x, y, x + width, y + imageHeight, PANEL_HIGHLIGHT);
                ThemeHelper.centeredText(graphics, this.font, Component.translatable("screen.music_and_melody.loading"), x + width / 2, y + imageHeight / 2 - 4, TEXT_DESCRIPTION);
            } else if (y >= top && y + imageHeight <= bottom) {
                graphics.blit(GUI_TEXTURED, loaded.texture(), x, y, 0.0F, 0.0F, width, imageHeight, loaded.width(), loaded.height());
            }
            return;
        }
        TextBlock text = (TextBlock) block;
        if (text.kind() == Kind.RULE) {
            graphics.fill(x, y + 3, x + width, y + 4, POPUP_OUTLINE);
            return;
        }
        if (text.kind() == Kind.SPACER) return;
        int color = text.kind() == Kind.QUOTE ? TEXT_DESCRIPTION : text.kind().heading ? TEXT_HEADER : TEXT_PRIMARY;
        Component component = inline(text.value());
        if (text.kind().heading) component = component.copy().withStyle(ChatFormatting.BOLD);
        List<FormattedCharSequence> lines = this.font.split(component, width);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = y + i * (this.font.lineHeight + 2);
            if (lineY >= top && lineY + this.font.lineHeight <= bottom) ThemeHelper.text(graphics, this.font, lines.get(i), x, lineY, color);
        }
    }

    private static MutableComponent inline(String text) {
        MutableComponent result = Component.empty();
        int cursor = 0;
        while (cursor < text.length()) {
            if (text.startsWith("**", cursor) || text.startsWith("__", cursor)) {
                String marker = text.substring(cursor, cursor + 2);
                int end = text.indexOf(marker, cursor + 2);
                if (end > cursor + 2) {
                    result.append(Component.literal(text.substring(cursor + 2, end)).withStyle(ChatFormatting.BOLD));
                    cursor = end + 2;
                    continue;
                }
            }
            if (text.charAt(cursor) == '*' || text.charAt(cursor) == '_') {
                char marker = text.charAt(cursor);
                int end = text.indexOf(marker, cursor + 1);
                if (end > cursor + 1) {
                    result.append(Component.literal(text.substring(cursor + 1, end)).withStyle(ChatFormatting.ITALIC));
                    cursor = end + 1;
                    continue;
                }
            }
            if (text.charAt(cursor) == '[') {
                int labelEnd = text.indexOf("](", cursor + 1);
                int urlEnd = labelEnd < 0 ? -1 : text.indexOf(')', labelEnd + 2);
                if (labelEnd > cursor + 1 && urlEnd > labelEnd + 2) {
                    result.append(Component.literal(text.substring(cursor + 1, labelEnd)).withStyle(ChatFormatting.UNDERLINE));
                    cursor = urlEnd + 1;
                    continue;
                }
            }
            int next = cursor + 1;
            while (next < text.length() && "*_[(".indexOf(text.charAt(next)) < 0) next++;
            result.append(Component.literal(text.substring(cursor, next)));
            cursor = next;
        }
        return result;
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, int mouseX, int mouseY) {
        int viewport = bottom - top;
        int thumb = Math.max(16, (int) Math.round(viewport * viewport / (viewport + this.scrollMax)));
        int thumbY = top + (int) Math.round((viewport - thumb) * this.scroll / this.scrollMax);
        graphics.fill(x, thumbY, x + 3, thumbY + thumb, mouseX >= x - 2 && mouseX <= x + 5 ? PANEL_HIGHLIGHT : POPUP_OUTLINE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = toLayoutMouse(mouseX), y = toLayoutMouse(mouseY);
        if (x >= dialogX() && x < dialogX() + dialogWidth() && y >= dialogY() + 31 && y < dialogY() + dialogHeight() - 35) {
            this.scroll = Math.max(0, Math.min(this.scrollMax, this.scroll - scrollY * 24));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(toLayoutMouse(event), doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(toLayoutMouse(event), dragX / MaMDataConfig.get().gui_multiplier, dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(toLayoutMouse(event));
    }

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    private int toLayoutMouse(double coordinate) {
        return Math.round((float) (coordinate / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int dialogWidth() { return Math.max(1, Math.min(620, this.layoutWidth - 24)); }
    private int dialogHeight() { return Math.max(1, this.layoutHeight - 24); }
    private int dialogX() { return this.layoutWidth / 2 - dialogWidth() / 2; }
    private int dialogY() { return this.layoutHeight / 2 - dialogHeight() / 2; }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private sealed interface Block permits TextBlock, ImageBlock {}
    private record TextBlock(String value, Kind kind) implements Block {}
    private record ImageBlock(String url) implements Block {}
    private enum Kind {
        BODY(false), QUOTE(false), SPACER(false), RULE(false), HEADING_LARGE(true), HEADING_MEDIUM(true), HEADING_SMALL(true);
        final boolean heading;
        Kind(boolean heading) { this.heading = heading; }
    }
}
