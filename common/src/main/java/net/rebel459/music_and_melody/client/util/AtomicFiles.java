package net.rebel459.music_and_melody.client.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFiles {

    private AtomicFiles() {}

    @FunctionalInterface
    public interface WriterAction {
        void write(Writer writer) throws IOException;
    }

    public static void write(Path path, WriterAction action) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp)) {
            action.write(writer);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temp);
            throw exception;
        }
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
