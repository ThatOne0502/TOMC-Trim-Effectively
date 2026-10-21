package io.github.thatone0502.trimeffectively.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.thatone0502.trimeffectively.Trimeffectively;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings read from {@code config/trimeffectively.json}.
 *
 * <p>The file lives on whoever is hosting the world. That is structural rather than a convention: every setting
 * here is consulted on the authoritative side only - the recipe setting from inside {@code RecipeManager#apply},
 * the reload listener of {@code ReloadableServerResources}, a class that exists on a dedicated server and on the
 * singleplayer host but not on a client that joined somebody else's server (a remote client never builds recipes
 * locally, it takes them from {@code ClientboundUpdateRecipesPacket}). So the host decides and the result reaches
 * the client through the ordinary recipe sync.
 *
 * <p>The config screen is a different matter: it can be opened on any client, including one connected to a
 * server it does not own. What the client shows there comes from {@code TreConfigPayload}, pushed by the host -
 * see {@link io.github.thatone0502.trimeffectively.net.TreConfigSync} - so a player cannot edit somebody else's
 * world settings by editing their own file.
 *
 * <p>The file is read again on every recipe reload, so editing it and running {@code /reload} is enough - no
 * restart, and no cached value to get out of step with what is on disk.
 */
public final class TreConfig {
	private static final String FILE_NAME = "trimeffectively.json";
	private static final String HIJACK_TRIM_DUPLICATION = "hijack_trim_duplication_recipes";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static boolean warned;
	/** The values last read or written by this side; only a fallback for screens opened before a payload arrives. */
	private static Values cached = Values.DEFAULT;

	/** One snapshot of the file. Add a field here and it automatically travels in the sync payload as well. */
	public record Values(boolean hijackTrimDuplicationRecipes) {
		public static final Values DEFAULT = new Values(true);

		public Values withHijackTrimDuplicationRecipes(boolean value) {
			return new Values(value);
		}
	}

	/** Where the file lives on this side. */
	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** Reads the file from disk, writing the default first if it is not there yet. */
	public static Values read() {
		Path file = file();
		if (!Files.isRegularFile(file)) {
			writeDefault(file);
			cached = Values.DEFAULT;
			return cached;
		}
		try {
			JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			boolean hijack = Values.DEFAULT.hijackTrimDuplicationRecipes();
			if (json.has(HIJACK_TRIM_DUPLICATION)) {
				hijack = json.get(HIJACK_TRIM_DUPLICATION).getAsBoolean();
			} else {
				// A file written before this key existed: add it instead of silently ignoring the file, so the
				// user can see what there is to change.
				json.addProperty(HIJACK_TRIM_DUPLICATION, hijack);
				Files.writeString(file, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
			}
			cached = new Values(hijack);
			return cached;
		} catch (IOException | RuntimeException e) {
			// A broken config must not take the server down, and must not silently change game balance either:
			// fall back to the default and say so once.
			if (!warned) {
				warned = true;
				Trimeffectively.LOGGER.warn("[tre] cannot read {}, using defaults: {}", file, e.toString());
			}
			cached = Values.DEFAULT;
			return cached;
		}
	}

	/** The last values this side saw, without touching the disk. */
	public static Values cached() {
		return cached;
	}

	/** Writes every setting, creating the directory if needed. Returns whether the file was actually written. */
	public static boolean write(Values values) {
		JsonObject json = new JsonObject();
		json.addProperty(HIJACK_TRIM_DUPLICATION, values.hijackTrimDuplicationRecipes());
		Path file = file();
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			Files.writeString(file, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
			cached = values;
			Trimeffectively.LOGGER.info("[tre] wrote config {} (hijack_trim_duplication_recipes={})",
					file, values.hijackTrimDuplicationRecipes());
			return true;
		} catch (IOException e) {
			Trimeffectively.LOGGER.warn("[tre] cannot write {}: {}", file, e.toString());
			return false;
		}
	}

	/**
	 * Whether the trim template duplication recipes (the vanilla {@code #S# / #C# / ###} shape) should be
	 * rewritten so the decoration and the pattern's own material trade places. Defaults to true.
	 */
	public static boolean hijackTrimDuplicationRecipes() {
		return read().hijackTrimDuplicationRecipes();
	}

	private static void writeDefault(Path file) {
		JsonObject json = new JsonObject();
		json.addProperty(HIJACK_TRIM_DUPLICATION, Values.DEFAULT.hijackTrimDuplicationRecipes());
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			Files.writeString(file, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
			Trimeffectively.LOGGER.info("[tre] wrote default config {}", file);
		} catch (IOException e) {
			Trimeffectively.LOGGER.warn("[tre] cannot write {}: {}", file, e.toString());
		}
	}

	private TreConfig() {
	}
}
