package com.pinyinspeaker;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class ItemSpeaker {

    private static Function libcSystem;
    private static boolean jnaInitialized = false;
    public static volatile boolean isReady = false;

    static {
        // Patch ProcessImpl.launchMechanism to FORK as an extra safeguard
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);

            Class<?> pClass = Class.forName("java.lang.ProcessImpl");
            java.lang.reflect.Field lmField = pClass.getDeclaredField("launchMechanism");
            Object base = unsafe.staticFieldBase(lmField);
            long offset = unsafe.staticFieldOffset(lmField);

            Class<?> enumClass = Class.forName("java.lang.ProcessImpl$LaunchMechanism");
            Object forkEnum = Enum.valueOf((Class<Enum>) enumClass, "FORK");
            unsafe.putObject(base, offset, forkEnum);
        } catch (Throwable ignored) {}
    }

    public static synchronized Function getLibcSystem() {
        if (!jnaInitialized) {
            try {
                libcSystem = NativeLibrary.getInstance("c").getFunction("system");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            jnaInitialized = true;
        }
        return libcSystem;
    }

    public static void warmUp() {
        CompletableFuture.runAsync(() -> {
            try {
                getLibcSystem();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    Function sys = getLibcSystem();
                    if (sys != null) {
                        sys.invokeInt(new Object[]{"/usr/bin/say -v Tingting '' 2>/dev/null &"});
                        sys.invokeInt(new Object[]{"/usr/bin/say -v Mónica '' 2>/dev/null &"});
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                isReady = true;
            }
        });
    }

    public static void speak(ItemStack stack) {
        DictionaryManager.Entry entry = DictionaryManager.resolve(stack);
        if (entry != null && entry.hanzi != null && !entry.hanzi.isEmpty()) {
            speakChinese(entry.hanzi);
        } else {
            String rawName = stack.getName().getString()
                    .replaceAll("§[0-9a-fk-or]", "")
                    .replaceAll("\\s*[\\(\\（][^\\)\\）]*[\\)\\）]", "")
                    .trim();
            speakChinese(rawName.isEmpty() ? stack.getName().getString() : rawName);
        }
    }

    public static void speakChinese(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!isReady) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§e[Pinyin Speaker] 正在准备语音... / Cargando motor de voz..."), true);
            }
        }
        String clean = text.replaceAll("[\\[\\]\\(\\)\\§\'\"]", "").trim();
        CompletableFuture.runAsync(() -> runTTS(clean, true));
    }

    public static void speakSpanish(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!isReady) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§e[Pinyin Speaker] 正在准备语音... / Cargando motor de voz..."), true);
            }
        }
        String clean = text.replaceAll("[\\[\\]\\(\\)\\§\'\"]", "").trim();
        CompletableFuture.runAsync(() -> runTTS(clean, false));
    }

    private static synchronized void runTTS(String text, boolean isChinese) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                String voice = isChinese ? "Tingting" : "Mónica";

                // Direct JNA libc system invocation - 100% reliable on macOS, bypasses posix_spawn
                Function sys = getLibcSystem();
                if (sys != null) {
                    String cmd = "killall say 2>/dev/null; /usr/bin/say -v " + voice + " " + escapeBash(text) + " &";
                    sys.invokeInt(new Object[]{cmd});
                    return;
                }

                // Fallback: ProcessBuilder
                new ProcessBuilder("/usr/bin/say", "-v", voice, text).start();
            } else if (os.contains("win")) {
                String culture = isChinese ? "zh-CN" : "es-ES";
                String safeText = text.replace("'", "''");
                String psCommand = "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.SelectVoiceByHints(0, 0, 0, [System.Globalization.CultureInfo]::GetCultureInfo('" + culture + "')); " +
                        "$s.Speak('" + safeText + "');";
                new ProcessBuilder("powershell", "-NoProfile", "-Command", psCommand).start();
            } else if (os.contains("linux")) {
                String voice = isChinese ? "zh" : "es";
                String cmd = "killall espeak-ng 2>/dev/null; killall espeak 2>/dev/null; (espeak-ng -v " + voice + " " + escapeBash(text) + " || espeak -v " + voice + " " + escapeBash(text) + " || spd-say -l " + voice + " " + escapeBash(text) + ") &";
                Function sys = getLibcSystem();
                if (sys != null) {
                    sys.invokeInt(new Object[]{cmd});
                    return;
                }
                new ProcessBuilder("espeak", "-v", voice, text).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String escapeBash(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
