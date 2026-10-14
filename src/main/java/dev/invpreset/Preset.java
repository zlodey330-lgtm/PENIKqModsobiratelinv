package dev.invpreset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Preset {
    /** Слоты PlayerScreenHandler: 5-8 броня, 9-35 инвентарь, 36-44 хотбар, 45 оффхенд. */
    public static final int FIRST = 5;
    public static final int LAST = 45;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** id предмета по номеру слота, null = пусто / не важно. */
    public String[] items = new String[LAST + 1];

    private static Preset instance = new Preset();

    public static Preset get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("invpreset_layout.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Preset s = GSON.fromJson(Files.readString(p), Preset.class);
                if (s != null && s.items != null && s.items.length == LAST + 1) instance = s;
            }
        } catch (Exception e) {
            InvPresetClient.LOG.warn("Не удалось прочитать пресет", e);
        }
    }

    private static void write() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            InvPresetClient.LOG.warn("Не удалось сохранить пресет", e);
        }
    }

    public boolean isEmpty() {
        for (int i = FIRST; i <= LAST; i++) if (items[i] != null) return false;
        return true;
    }

    public Item itemAt(int slot) {
        String id = items[slot];
        if (id == null) return null;
        Identifier ident = Identifier.tryParse(id);
        if (ident == null || !Registries.ITEM.containsId(ident)) return null;
        return Registries.ITEM.get(ident);
    }

    public static void saveFrom(MinecraftClient mc) {
        if (mc.player == null) return;
        ScreenHandler h = mc.player.playerScreenHandler;
        Preset p = new Preset();
        int count = 0;
        for (int i = FIRST; i <= LAST; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (!s.isEmpty()) {
                p.items[i] = Registries.ITEM.getId(s.getItem()).toString();
                count++;
            }
        }
        instance = p;
        write();
        mc.player.sendMessage(Text.literal("[InvPreset] Пресет сохранён (" + count + " слотов)"), true);
    }
}
