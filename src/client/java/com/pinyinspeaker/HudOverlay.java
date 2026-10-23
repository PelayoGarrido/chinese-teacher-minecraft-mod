package com.pinyinspeaker;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class HudOverlay {

    public static ItemStack lastHoveredStack = ItemStack.EMPTY;
    public static long lastHoveredTime = 0;

    public static void updateHovered(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            lastHoveredStack = stack.copy();
            lastHoveredTime = System.currentTimeMillis();
        }
    }

    public static ItemStack getActiveHoveredStack() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            return ItemStack.EMPTY;
        }
        // Valid if hovered within the last 600ms
        if (System.currentTimeMillis() - lastHoveredTime < 600) {
            return lastHoveredStack;
        }
        return ItemStack.EMPTY;
    }

    public static DictionaryManager.Entry getCurrentEntry(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;

        // 1. If an inventory / creative / recipe book screen is open:
        if (client.currentScreen != null) {
            ItemStack hovered = getActiveHoveredStack();
            if (hovered != null && !hovered.isEmpty()) {
                return DictionaryManager.resolve(hovered);
            }
        }

        // 2. If in-game (no screen open):
        if (client.currentScreen == null) {
            HitResult hit = client.crosshairTarget;
            if (hit != null) {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    BlockState state = client.world.getBlockState(bhr.getBlockPos());
                    if (!state.isAir()) {
                        DictionaryManager.Entry entry = DictionaryManager.resolve(state);
                        if (entry != null) return entry;
                    }
                } else if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult ehr = (EntityHitResult) hit;
                    Entity entity = ehr.getEntity();
                    DictionaryManager.Entry entry = DictionaryManager.resolve(entity);
                    if (entry != null) return entry;
                }
            }

            // Fallback: Item held in main hand
            ItemStack hand = client.player.getMainHandStack();
            if (hand != null && !hand.isEmpty()) {
                return DictionaryManager.resolve(hand);
            }
        }

        return null;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.getDebugHud().shouldShowDebugHud()) return;

        // Don't render inside non-gameplay screens (e.g. main menu, options, video settings)
        if (client.currentScreen != null && client.world == null) {
            return;
        }

        DictionaryManager.Entry entry = getCurrentEntry(client);
        if (entry == null || entry.hanzi == null || entry.hanzi.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();

        String line1 = "§6§l[ " + entry.hanzi + " ]  §e§l" + entry.pinyin;
        String line2 = (entry.spanish != null && !entry.spanish.isEmpty())
                ? "§7Español: §f" + entry.spanish
                : "";

        int w1 = textRenderer.getWidth(line1);
        int w2 = line2.isEmpty() ? 0 : textRenderer.getWidth(line2);
        int boxWidth = Math.max(w1, w2) + 20;
        int boxHeight = line2.isEmpty() ? 18 : 28;

        int x = (screenWidth - boxWidth) / 2;
        int y = 12;

        // Dark background box with subtle top accent line
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xB011161F);
        context.fill(x, y, x + boxWidth, y + 2, 0xFFFFAA00);

        int textX1 = x + (boxWidth - w1) / 2;
        context.drawText(textRenderer, line1, textX1, y + 5, 0xFFFFFF, true);

        if (!line2.isEmpty()) {
            int textX2 = x + (boxWidth - w2) / 2;
            context.drawText(textRenderer, line2, textX2, y + 16, 0xDDDDDD, true);
        }
    }
}
