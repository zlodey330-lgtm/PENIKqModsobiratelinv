package dev.invpreset;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class Restorer {
    /** a = целевой слот, b = откуда берём. itemA == null, если в a пусто. */
    private record Move(int a, int b, Item itemA, Item itemB) {
        int clicks() {
            return itemA == null ? 2 : 3;
        }
    }

    private static boolean active = false;
    private static List<Move> moves = new ArrayList<>();
    private static int idx = 0;
    private static int step = 0;
    private static long nextAt = 0;
    private static long baseDelayMs = 0;

    private Restorer() {}

    public static boolean isActive() {
        return active;
    }

    public static void start(MinecraftClient mc) {
        if (active) return;
        if (mc.player == null || !(mc.currentScreen instanceof InventoryScreen)) return;

        Preset preset = Preset.get();
        if (preset.isEmpty()) {
            msg(mc, "[InvPreset] Пресет не сохранён (Shift+K)");
            return;
        }
        ScreenHandler h = mc.player.playerScreenHandler;
        if (!h.getCursorStack().isEmpty()) {
            msg(mc, "[InvPreset] Сначала убери предмет с курсора");
            return;
        }

        // Планируем перестановки на копии инвентаря
        ItemStack[] cur = new ItemStack[Preset.LAST + 1];
        Item[] want = new Item[Preset.LAST + 1];
        for (int i = Preset.FIRST; i <= Preset.LAST; i++) {
            cur[i] = h.getSlot(i).getStack().copy();
            want[i] = preset.itemAt(i);
        }

        List<Move> plan = new ArrayList<>();
        for (int i = Preset.FIRST; i <= Preset.LAST; i++) {
            Item w = want[i];
            if (w == null) continue;
            if (!cur[i].isEmpty() && cur[i].getItem() == w) continue;

            int best = -1;
            for (int j = Preset.FIRST; j <= Preset.LAST; j++) {
                if (j == i || cur[j].isEmpty() || cur[j].getItem() != w) continue;
                if (want[j] != null && cur[j].getItem() == want[j]) continue; // уже на своём месте
                if (!h.getSlot(i).canInsert(cur[j])) continue;
                if (!cur[i].isEmpty() && !h.getSlot(j).canInsert(cur[i])) continue;
                if (best == -1) best = j;
                if (!cur[i].isEmpty() && want[j] != null && want[j] == cur[i].getItem()) {
                    best = j; // идеальный обмен
                    break;
                }
            }
            if (best == -1) continue;

            plan.add(new Move(i, best, cur[i].isEmpty() ? null : cur[i].getItem(), cur[best].getItem()));
            ItemStack tmp = cur[i];
            cur[i] = cur[best];
            cur[best] = tmp;
        }

        if (plan.isEmpty()) {
            msg(mc, "[InvPreset] Всё уже на своих местах");
            return;
        }

        int totalClicks = 0;
        for (Move m : plan) totalClicks += m.clicks();

        long totalMs = Math.max(Config.MIN_SECONDS, Config.get().totalSeconds) * 1000L;
        baseDelayMs = Math.max(1L, totalMs / totalClicks);

        moves = plan;
        idx = 0;
        step = 0;
        active = true;
        nextAt = System.currentTimeMillis() + nextDelay();
        msg(mc, "[InvPreset] Расставляю: " + totalClicks + " кликов, ~" + (totalMs / 1000) + " сек. Не закрывай инвентарь");
    }

    /** Задержка только в большую сторону, чтобы суммарно не уйти ниже минимума. */
    private static long nextDelay() {
        return (long) (baseDelayMs * (1.0 + Math.random() * 0.2));
    }

    public static void tick(MinecraftClient mc) {
        if (!active) return;
        if (mc.player == null || mc.interactionManager == null || !(mc.currentScreen instanceof InventoryScreen)) {
            reset();
            return;
        }
        if (System.currentTimeMillis() < nextAt) return;

        Move m = moves.get(idx);
        ScreenHandler h = mc.player.playerScreenHandler;

        if (step == 0) {
            boolean ok = h.getCursorStack().isEmpty()
                    && !h.getSlot(m.b()).getStack().isEmpty()
                    && h.getSlot(m.b()).getStack().getItem() == m.itemB()
                    && (m.itemA() == null
                        ? h.getSlot(m.a()).getStack().isEmpty()
                        : h.getSlot(m.a()).getStack().getItem() == m.itemA());
            if (!ok) {
                msg(mc, "[InvPreset] Инвентарь изменился, остановлено. Нажми K ещё раз");
                reset();
                return;
            }
        }

        int slot = (step == 1) ? m.a() : m.b();
        click(mc, slot);
        step++;
        if (step >= m.clicks()) {
            step = 0;
            idx++;
        }

        if (idx >= moves.size()) {
            msg(mc, "[InvPreset] Готово");
            reset();
            return;
        }
        nextAt = System.currentTimeMillis() + nextDelay();
    }

    /** Вызывается при закрытии инвентаря. */
    public static void cancel(MinecraftClient mc) {
        if (!active) return;
        reset();
    }

    private static void reset() {
        active = false;
        moves = new ArrayList<>();
        idx = 0;
        step = 0;
    }

    private static void click(MinecraftClient mc, int slot) {
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    private static void msg(MinecraftClient mc, String text) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), true);
    }
}
