package net.rebel459.music_and_melody.client.element;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.client.util.MusicScreenHelper;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Set;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

public final class RemoteDetailsPanel {
    private RemoteDetailsPanel() {}

    @FunctionalInterface
    public interface MarqueeRenderer {
        void render(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color);
    }

    public static double render(GuiGraphicsExtractor graphics, Minecraft minecraft, Font font, RemotePack pack, int rightX, int rightWidth, int panelTop, int panelBottom, boolean deletePending, double scroll, MarqueeRenderer marquee) {
        int x = rightX + 8;
        int width = rightWidth - 16;
        ThemeHelper.text(graphics, font, Component.translatable("screen.music_and_melody.details").withStyle(ChatFormatting.BOLD), x, panelTop + 14, TEXT_HEADER);
        int iconSize = Math.min(42, width);
        int iconY = panelTop + 30;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(minecraft, RemoteIconManager.icon(pack)), x, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, width - iconSize - 6);
        marquee.render(graphics, pack.name(), textX, iconY + 1, textWidth, deletePending ? TEXT_PENDING_DELETION : TEXT_TITLE);
        marquee.render(graphics, Component.literal(pack.id().getNamespace() + ":"), textX, iconY + 13, textWidth, TEXT_DESCRIPTION);
        marquee.render(graphics, Component.literal(pack.id().getPath()), textX, iconY + 25, textWidth, TEXT_DESCRIPTION);

        int bodyTop = iconY + iconSize + 5;
        int bodyBottom = panelBottom - 62;
        Body body = body(font, pack, width);
        double maxScroll = Math.max(0.0D, body.height() - Math.max(1, bodyBottom - bodyTop));
        scroll = Math.max(0.0D, Math.min(scroll, maxScroll));
        int offset = (int) Math.round(scroll);

        graphics.enableScissor(rightX + 2, bodyTop, rightX + rightWidth - 2, bodyBottom);
        int y = bodyTop - offset;
        renderField(graphics, font, marquee, "screen.music_and_melody.remote_details.repository", Component.literal(pack.repository()), x, y, width);
        renderField(graphics, font, marquee, "screen.music_and_melody.remote_details.version", Component.literal(pack.version()), x, y + 26, width);
        renderField(graphics, font, marquee, "screen.music_and_melody.remote_details.state", Component.translatable(MusicPlayerScreen.remoteStateTranslationKey(RemoteContentManager.state(pack))), x, y + 52, width);

        renderField(graphics, font, marquee, sizeKey(RemoteContentManager.state(pack)), sizeText(pack), x, y + 78, width);

        int cursor = y + 104;
        marquee.render(graphics, Component.translatable("screen.music_and_melody.theme.description").withStyle(ChatFormatting.UNDERLINE), x, cursor, width, TEXT_DESCRIPTION);
        cursor += 12;
        for (FormattedCharSequence line : body.description()) {
            ThemeHelper.text(graphics, font, line, x, cursor, TEXT_PRIMARY);
            cursor += font.lineHeight + 2;
        }

        if (!body.dependencies().isEmpty()) {
            cursor += 6;
            marquee.render(graphics, Component.translatable("screen.music_and_melody.remote_details.dependencies").withStyle(ChatFormatting.UNDERLINE), x, cursor, width, TEXT_DESCRIPTION);
            cursor += 12;
            for (FormattedCharSequence line : body.dependencies()) {
                ThemeHelper.text(graphics, font, line, x, cursor, TEXT_PRIMARY);
                cursor += font.lineHeight + 2;
            }
        }
        graphics.disableScissor();

        if (maxScroll > 0.0D) {
            int viewport = Math.max(1, bodyBottom - bodyTop);
            int thumbHeight = Math.max(16, (int) Math.round(viewport * viewport / (viewport + maxScroll)));
            int travel = Math.max(1, viewport - thumbHeight);
            int thumbY = bodyTop + (int) Math.round(travel * scroll / maxScroll);
            int scrollbarX = rightX + rightWidth - 5;
            graphics.fill(scrollbarX, bodyTop, scrollbarX + 2, bodyBottom, BAR_BACKGROUND);
            graphics.fill(scrollbarX - 1, thumbY, scrollbarX + 3, thumbY + thumbHeight, SCROLLBAR_THUMB);
        }

        OptionalDouble progress = RemoteContentManager.downloadProgress(pack);
        if (progress.isPresent()) {
            int right = rightX + rightWidth - 8;
            int progressY = panelBottom - 47;
            graphics.fill(x, progressY, right, progressY + 4, BAR_BACKGROUND);
            graphics.fill(x, progressY, x + (int) Math.round((right - x) * progress.getAsDouble()), progressY + 4, PANEL_HIGHLIGHTED);
        }
        return maxScroll;
    }

    private static Body body(Font font, RemotePack pack, int width) {
        List<FormattedCharSequence> description = font.split(pack.description(), Math.max(1, width));
        List<FormattedCharSequence> dependencies = pack.dependencies().isEmpty()
                ? List.of() : font.split(dependencies(pack), Math.max(1, width));
        int height = 104 + 12 + description.size() * (font.lineHeight + 2);
        if (!dependencies.isEmpty()) height += 6 + 12 + dependencies.size() * (font.lineHeight + 2);
        return new Body(description, dependencies, height);
    }

    private static String sizeKey(RemoteContentManager.State state) {
        return switch (state) {
            case INSTALLED, NEEDS_RELOAD -> "screen.music_and_melody.remote_details.size.current";
            case UPDATE_AVAILABLE -> "screen.music_and_melody.remote_details.size.update";
            default -> "screen.music_and_melody.remote_details.size.download_install";
        };
    }

    private static Component sizeText(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        OptionalLong current = RemoteContentManager.installedSize(pack);
        Component currentSize = formatSize(current.isPresent() ? current.getAsLong() : 0L);
        return switch (state) {
            case INSTALLED, NEEDS_RELOAD -> currentSize;
            case UPDATE_AVAILABLE -> join(formatSize(pack.downloadSize()), formatSize(pack.installSize()), currentSize);
            default -> join(formatSize(pack.downloadSize()), formatSize(pack.installSize()));
        };
    }

    private static Component join(Component... parts) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(Component.literal(" / "));
            result.append(parts[i]);
        }
        return result;
    }

    private static Component formatSize(long bytes) {
        if (bytes <= 0L) return Component.translatable("screen.music_and_melody.remote_details.size.unknown");
        if (bytes < 1024L) return Component.literal(bytes + " B");
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        while (value >= 1024.0D && unit < units.length - 1) {
            value /= 1024.0D;
            unit++;
        }
        return Component.literal(String.format(Locale.ROOT, value >= 100.0D ? "%.0f %s" : "%.1f %s", value, units[unit]));
    }

    private static Component dependencies(RemotePack pack) {
        Set<String> missing = new HashSet<>(RemoteContentManager.missingDependencies(pack));
        MutableComponent result = Component.empty();
        for (int i = 0; i < pack.dependencies().size(); i++) {
            if (i > 0) result.append(Component.literal(", "));
            String dependency = pack.dependencies().get(i);
            MutableComponent name = Component.literal(dependency);
            if (missing.contains(dependency)) name.withStyle(style -> style.withColor(ThemeHelper.rgb(TEXT_PENDING_DELETION)));
            result.append(name);
        }
        return result;
    }

    private static void renderField(GuiGraphicsExtractor graphics, Font font, MarqueeRenderer marquee, String headingKey, Component value, int x, int y, int width) {
        marquee.render(graphics, Component.translatable(headingKey).withStyle(ChatFormatting.UNDERLINE), x, y, width, TEXT_DESCRIPTION);
        marquee.render(graphics, value, x, y + 12, width, TEXT_PRIMARY);
    }

    private record Body(List<FormattedCharSequence> description, List<FormattedCharSequence> dependencies, int height) {}
}
