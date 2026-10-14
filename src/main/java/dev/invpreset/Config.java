package dev.invpreset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    /** Нижняя граница: общее время восстановления, секунд. Меньше нельзя. */
    public static final int MIN_SECONDS = 45;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Общее время, за которое мод расставит все предметы (сек). */
    public int totalSeconds = MIN_SECONDS;

    private static Config instance = new Config();

    public static Config get() {
        return instance;
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
        instance.totalSeconds = Math.max(MIN_SECONDS, instance.totalSeconds);
        save();
    }

    public static void save() {
        try {
            instance.totalSeconds = Math.max(MIN_SECONDS, instance.totalSeconds);
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            InvPresetClient.LOG.warn("Не удалось сохранить конфиг", e);
        }
    }
}
