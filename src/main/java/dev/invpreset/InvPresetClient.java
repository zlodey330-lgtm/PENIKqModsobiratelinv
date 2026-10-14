package dev.invpreset;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvPresetClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("invpreset");

    @Override
    public void onInitializeClient() {
        Config.load();
        Preset.load();

        // K / Shift+K работают только в открытом инвентаре
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof InventoryScreen) {
                ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, mods) -> {
                    if (key == GLFW.GLFW_KEY_K) {
                        if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) {
                            Preset.saveFrom(MinecraftClient.getInstance());
                        } else {
                            Restorer.start(MinecraftClient.getInstance());
                        }
                        return false;
                    }
                    return true;
                });
                ScreenEvents.remove(screen).register(s -> Restorer.cancel(MinecraftClient.getInstance()));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(Restorer::tick);

        // /invpreset delay <секунды>  (от 5 до 50)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("invpreset")
                        .then(ClientCommandManager.literal("delay")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[InvPreset] Время восстановления: " + Config.get().totalSeconds
                                                    + " сек (допустимо " + Config.MIN_SECONDS + "-" + Config.MAX_SECONDS + ")"));
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(Config.MIN_SECONDS, Config.MAX_SECONDS))
                                        .executes(ctx -> {
                                            int v = IntegerArgumentType.getInteger(ctx, "seconds");
                                            Config.get().totalSeconds = v;
                                            Config.save();
                                            ctx.getSource().sendFeedback(Text.literal("[InvPreset] Время восстановления: " + v + " сек"));
                                            return 1;
                                        })))));
    }
}
