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

    private final MinecraftClient client;
    private final KeyBinding key;
    private final OnConfirm onConfirm;

    public ManualConfirmKeybind(MinecraftClient client, OnConfirm onConfirm) {
        if (client == null) throw new NullPointerException("client");
        if (onConfirm == null) throw new NullPointerException("onConfirm");

        this.client = client;
        this.onConfirm = onConfirm;

        this.key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nozh.apply_suggestion",
                GLFW.GLFW_KEY_K,
                "category.nozh"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient c) {
        if (c != client) return;
        while (key.wasPressed()) {
            onConfirm.onConfirm();
        }
    }
}
