package com.example.simpleesp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class SimpleEspClient implements ClientModInitializer {

    private enum Mode {
        ALL("All living"),
        PLAYERS("Players only"),
        HOSTILE("Hostile mobs only");

        final String label;

        Mode(String label) {
            this.label = label;
        }

        Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    private static boolean enabled = false;
    private static Mode mode = Mode.ALL;

    private static final Set<Entity> glowed = new HashSet<>();

    @Override
    public void onInitializeClient() {
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Simple ESP: Toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyBinding.Category.MISC));
        KeyBinding modeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Simple ESP: Cycle mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                say(client, "ESP " + (enabled ? "ON" : "OFF") + " (" + mode.label + ")");
            }
            while (modeKey.wasPressed()) {
                mode = mode.next();
                say(client, "ESP mode: " + mode.label);
            }
            update(client);
        });
    }

    private static void update(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            glowed.clear();
            return;
        }

        for (Entity e : glowed) {
            if (!shouldHighlight(client, e)) e.setGlowing(false);
        }
        glowed.removeIf(e -> e.isRemoved() || !shouldHighlight(client, e));

        if (!enabled) return;

        for (Entity e : client.world.getEntities()) {
            if (shouldHighlight(client, e)) {
                e.setGlowing(true);
                glowed.add(e);
            }
        }
    }

    private static boolean shouldHighlight(MinecraftClient client, Entity e) {
        if (!enabled || e == client.player) return false;
        if (!(e instanceof LivingEntity) || e instanceof ArmorStandEntity) return false;
        return switch (mode) {
            case ALL -> true;
            case PLAYERS -> e instanceof PlayerEntity;
            case HOSTILE -> e instanceof Monster;
        };
    }

    private static void say(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Simple ESP] " + message), true);
        }
    }
}
