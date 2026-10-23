package com.pinyinspeakerlearninglilcremes.client;

import com.pinyinspeaker.DictionaryManager;
import com.pinyinspeaker.HudOverlay;
import com.pinyinspeaker.ItemSpeaker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PinyinSpeakerClient implements ClientModInitializer {

    public static KeyBinding speakChineseKey;
    public static KeyBinding speakSpanishKey;

    @Override
    public void onInitializeClient() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            DictionaryManager.init();
            ItemSpeaker.warmUp();
        });

        speakChineseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinyin-speaker.speak_chinese",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.pinyin-speaker"
        ));

        speakSpanishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinyin-speaker.speak_spanish",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.pinyin-speaker"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            if (client.currentScreen == null && client.isWindowFocused() && !client.mouse.isCursorLocked()) {
                client.mouse.lockCursor();
            }

            if (client.currentScreen == null) {
                while (speakChineseKey.wasPressed()) {
                    DictionaryManager.Entry entry = HudOverlay.getCurrentEntry(client);
                    if (entry != null && entry.hanzi != null && !entry.hanzi.isEmpty()) {
                        ItemSpeaker.speakChinese(entry.hanzi);
                    }
                }

                while (speakSpanishKey.wasPressed()) {
                    DictionaryManager.Entry entry = HudOverlay.getCurrentEntry(client);
                    if (entry != null && entry.spanish != null && !entry.spanish.isEmpty()) {
                        ItemSpeaker.speakSpanish(entry.spanish);
                        client.player.sendMessage(
                                Text.literal("§6[" + entry.hanzi + "] §e" + entry.pinyin + " §7-> §a" + entry.spanish), true);
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            HudOverlay.render(drawContext);
        });

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack == null || stack.isEmpty()) return;
            HudOverlay.updateHovered(stack);
            DictionaryManager.Entry entry = DictionaryManager.resolve(stack);
            if (entry != null && entry.hanzi != null && !entry.hanzi.isEmpty()) {
                lines.add(1, Text.literal("§6[汉字] §f" + entry.hanzi + "  §e" + entry.pinyin));
                if (entry.spanish != null && !entry.spanish.isEmpty()) {
                    lines.add(2, Text.literal("§7[Español] §a" + entry.spanish));
                }
            }
        });
    }
}