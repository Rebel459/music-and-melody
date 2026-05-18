package net.rebel459.music_and_melody.network;

public final class ServerHelper {

    private static boolean absent;
    public static boolean countDiscUses;

    private ServerHelper() {}

    public static void reset() {
        absent = false;
    }

    public static void markPresent() {
        absent = false;
    }

    public static void markAbsent() {
        absent = true;
    }

    public static boolean isAbsent() {
        return absent;
    }
}
