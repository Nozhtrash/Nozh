package dev.nozh.core.manual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class ManualConfirmKeybind {

    public interface OnConfirm {
        void onConfirm();
    }

    // Static registration ensures it's only registered once by the class loader
    // interaction or static init
    private static final KeyBinding KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nozh.apply_suggestion",
            GLFW.GLFW_KEY_K,
            "category.nozh"));

    private final MinecraftClient client;
    private final OnConfirm onConfirm;

    public ManualConfirmKeybind(MinecraftClient client, OnConfirm onConfirm) {
        if (client == null)
            throw new NullPointerException("client");
        if (onConfirm == null)
            throw new NullPointerException("onConfirm");

        this.client = client;
        this.onConfirm = onConfirm;

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient c) {
        if (c != client)
            return;
        while (KEY.wasPressed()) {
            onConfirm.onConfirm();
        }
    }
}
