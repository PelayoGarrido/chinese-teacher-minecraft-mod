package com.pinyinspeaker.mixin.client;

import com.pinyinspeaker.DictionaryManager;
import com.pinyinspeaker.HudOverlay;
import com.pinyinspeaker.ItemSpeaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    private static boolean isAnyTextFieldFocused(Element element) {
        if (element == null) return false;
        if (element instanceof TextFieldWidget textField && textField.isFocused()) {
            return true;
        }
        if (element instanceof ParentElement parent) {
            return isAnyTextFieldFocused(parent.getFocused());
        }
        return false;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onScreenKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Do not intercept if user is actively typing in any search bar (Creative Search, Recipe Book, Anvil, etc.)
        if ((Object) this instanceof ParentElement parent && isAnyTextFieldFocused(parent.getFocused())) {
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_V || keyCode == GLFW.GLFW_KEY_B) {
            MinecraftClient client = MinecraftClient.getInstance();
            DictionaryManager.Entry entry = HudOverlay.getCurrentEntry(client);

            if (entry != null && entry.hanzi != null && !entry.hanzi.isEmpty()) {
                if (keyCode == GLFW.GLFW_KEY_V) {
                    ItemSpeaker.speakChinese(entry.hanzi);
                    cir.setReturnValue(true);
                } else {
                    if (entry.spanish != null && !entry.spanish.isEmpty()) {
                        ItemSpeaker.speakSpanish(entry.spanish);
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("§6[" + entry.hanzi + "] §e" + entry.pinyin + " §7-> §a" + entry.spanish), true);
                        }
                    }
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onScreenRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HudOverlay.render(context);
    }
}
