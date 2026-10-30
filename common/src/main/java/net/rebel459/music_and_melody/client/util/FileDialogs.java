package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryUtil.*;

public final class FileDialogs {

    private FileDialogs() {}

    public static void openFiles(@Nullable String defaultLocation, String filterName, List<String> extensions, boolean multiple, Consumer<List<Path>> callback) {
        Minecraft client = Minecraft.getInstance();
        ByteBuffer name = memUTF8(filterName);
        ByteBuffer pattern = memUTF8(String.join(";", extensions));
        SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
        filters.get(0).name(name).pattern(pattern);

        SDL_DialogFileCallback[] holder = new SDL_DialogFileCallback[1];
        holder[0] = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
            List<Path> selected = new ArrayList<>();
            if (filelist != NULL) {
                for (long entry = filelist; memGetAddress(entry) != NULL; entry += Pointer.POINTER_SIZE) {
                    try {
                        selected.add(Path.of(memUTF8(memGetAddress(entry))));
                    } catch (RuntimeException ignored) {}
                }
            }
            client.execute(() -> {
                holder[0].free();
                filters.free();
                memFree(name);
                memFree(pattern);
                callback.accept(selected);
            });
        });

        try {
            SDLDialog.SDL_ShowOpenFileDialog(holder[0], NULL, client.getWindow().handle(), filters,
                    defaultLocation, multiple);
        } catch (RuntimeException exception) {
            holder[0].free();
            filters.free();
            memFree(name);
            memFree(pattern);
            callback.accept(List.of());
        }
    }
}
