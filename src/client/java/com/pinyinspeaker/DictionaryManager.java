package com.pinyinspeaker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryManager {

    public static class Entry {
        public final String hanzi;
        public final String pinyin;
        public final String spanish;

        public Entry(String hanzi, String pinyin, String spanish) {
            this.hanzi = hanzi;
            this.pinyin = pinyin;
            this.spanish = spanish;
        }
    }

    private static final Map<String, Entry> DICTIONARY = new HashMap<>();
    private static final Map<String, String> CHAR_PINYIN = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        try {
            Gson gson = new Gson();

            // Load minecraft_dict.json
            InputStream dictStream = DictionaryManager.class.getResourceAsStream("/assets/pinyin-speaker/data/minecraft_dict.json");
            if (dictStream != null) {
                try (Reader reader = new InputStreamReader(dictStream, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
                    Map<String, List<String>> map = gson.fromJson(reader, type);
                    if (map != null) {
                        for (Map.Entry<String, List<String>> e : map.entrySet()) {
                            List<String> list = e.getValue();
                            if (list != null && list.size() >= 3) {
                                DICTIONARY.put(e.getKey(), new Entry(list.get(0), list.get(1), list.get(2)));
                            }
                        }
                    }
                }
            }

            // Load chars_pinyin.json
            InputStream charStream = DictionaryManager.class.getResourceAsStream("/assets/pinyin-speaker/data/chars_pinyin.json");
            if (charStream != null) {
                try (Reader reader = new InputStreamReader(charStream, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> map = gson.fromJson(reader, type);
                    if (map != null) {
                        CHAR_PINYIN.putAll(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public static Entry resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        init();

        String key = stack.getItem().getTranslationKey();
        Entry entry = DICTIONARY.get(key);
        if (entry != null) return entry;

        // Fallback for custom-named or mod items
        String rawName = stack.getName().getString()
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("\\s*[\\(\\（][^\\)\\）]*[\\)\\）]", "")
                .trim();

        if (hasHanzi(rawName)) {
            String pinyin = toPinyin(rawName);
            return new Entry(rawName, pinyin, rawName);
        }
        return new Entry(rawName, "", rawName);
    }

    public static Entry resolve(BlockState state) {
        if (state == null) return null;
        init();

        String itemKey = state.getBlock().asItem().getTranslationKey();
        Entry entry = DICTIONARY.get(itemKey);
        if (entry != null) return entry;

        String blockKey = state.getBlock().getTranslationKey();
        return DICTIONARY.get(blockKey);
    }

    public static Entry resolve(Entity entity) {
        if (entity == null) return null;
        init();

        String key = entity.getType().getTranslationKey();
        return DICTIONARY.get(key);
    }

    public static boolean hasHanzi(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') return true;
        }
        return false;
    }

    public static String toPinyin(String text) {
        init();
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            String py = CHAR_PINYIN.get(String.valueOf(c));
            if (py != null) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                sb.append(py).append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
