package dev.invpreset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    /** Диапазон общего времени восстановления, секунд: от 5 до 50. */
    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 50;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Общее время, за которое мод расставит все предметы (сек). */
    public int totalSeconds = MIN_SECONDS;

    private static Config instance = new Config();

    public static Config get() {
        return instance;
    }

    private static int clamp(int v) {
        return Math.min(MAX_SECONDS, Math.max(MIN_SECONDS, v));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("invpreset.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            InvPresetClient.LOG.warn("Не удалось прочитать конфиг", e);
        }
        instance.totalSeconds = clamp(instance.totalSeconds);
        save();
    }

    public static void save() {
        try {
            instance.totalSeconds = clamp(instance.totalSeconds);
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            InvPresetClient.LOG.warn("Не удалось сохранить конфиг", e);
        }
    }
}
 
