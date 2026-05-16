package net.rebel459.legacies_and_legends;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaLConstants {
	public static final String MOD_ID = "legacies_and_legends";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static Identifier vanillaId(String path) {
		return Identifier.withDefaultNamespace(path);
	}

	public static String string(@NotNull String path) {
		return LaLConstants.id(path).toString();
	}

}
