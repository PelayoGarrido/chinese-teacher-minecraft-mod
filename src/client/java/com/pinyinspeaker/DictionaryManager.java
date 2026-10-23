package com.pinyinspeaker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

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
    private static final Map<String, Entry> UI_DICTIONARY = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        try {
            Gson gson = new Gson();

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
            initUiDictionary();
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

    public static Entry resolveWidget(ClickableWidget widget) {
        if (widget == null || widget instanceof TextFieldWidget) return null;
        Text message = widget.getMessage();
        if (message == null) return null;

        init();

        if (message.getContent() instanceof TranslatableTextContent ttc) {
            String key = ttc.getKey();
            Entry entry = UI_DICTIONARY.get(key.toLowerCase());
            if (entry != null) return entry;
        }

        return resolveUiString(message.getString());
    }

    public static Entry resolveUiString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;

        String clean = raw.replaceAll("§[0-9a-fk-or]", "").trim();
        if (clean.isEmpty()) return null;

        String norm = clean.toLowerCase();

        Entry direct = UI_DICTIONARY.get(norm);
        if (direct != null) return direct;

        String strippedDots = clean.replaceAll("[\\.…]*$", "").trim();
        Entry noDots = UI_DICTIONARY.get(strippedDots.toLowerCase());
        if (noDots != null) return noDots;

        if (strippedDots.contains(":")) {
            int idx = strippedDots.indexOf(':');
            String left = strippedDots.substring(0, idx).trim();
            String right = strippedDots.substring(idx + 1).trim();

            Entry leftEntry = UI_DICTIONARY.get(left.toLowerCase());
            Entry rightEntry = UI_DICTIONARY.get(right.toLowerCase());

            if (leftEntry != null || rightEntry != null) {
                String hanziLeft = leftEntry != null ? leftEntry.hanzi : (hasHanzi(left) ? left : left);
                String pinyinLeft = leftEntry != null ? leftEntry.pinyin : (hasHanzi(left) ? toPinyin(left) : left);
                String spanishLeft = leftEntry != null ? leftEntry.spanish : left;

                String hanziRight = rightEntry != null ? rightEntry.hanzi : (hasHanzi(right) ? right : right);
                String pinyinRight = rightEntry != null ? rightEntry.pinyin : (hasHanzi(right) ? toPinyin(right) : right);
                String spanishRight = rightEntry != null ? rightEntry.spanish : right;

                return new Entry(
                        hanziLeft + "：" + hanziRight,
                        pinyinLeft + " : " + pinyinRight,
                        spanishLeft + ": " + spanishRight
                );
            }
        }

        if (hasHanzi(clean)) {
            String pinyin = toPinyin(clean);
            return new Entry(clean, pinyin, clean);
        }

        return null;
    }

    private static void putUi(String key, String hanzi, String pinyin, String spanish) {
        Entry e = new Entry(hanzi, pinyin, spanish);
        if (key != null) UI_DICTIONARY.put(key.toLowerCase(), e);
        if (spanish != null) UI_DICTIONARY.put(spanish.toLowerCase(), e);
        if (hanzi != null) UI_DICTIONARY.put(hanzi, e);
    }

    private static void initUiDictionary() {
        if (!UI_DICTIONARY.isEmpty()) return;

        putUi("menu.options", "选项", "xuǎn xiàng", "Opciones");
        putUi("options", "选项", "xuǎn xiàng", "Opciones");
        putUi("menu.returnToGame", "返回游戏", "fǎn huí yóu xì", "Volver al juego");
        putUi("back to game", "返回游戏", "fǎn huí yóu xì", "Volver al juego");
        putUi("menu.game", "游戏菜单", "yóu xì cài dān", "Menú del juego");
        putUi("game menu", "游戏菜单", "yóu xì cài dān", "Menú del juego");
        putUi("menu.singleplayer", "单人游戏", "dān rén yóu xì", "Un jugador");
        putUi("singleplayer", "单人游戏", "dān rén yóu xì", "Un jugador");
        putUi("menu.multiplayer", "多人游戏", "duō rén yóu xì", "Multijugador");
        putUi("multiplayer", "多人游戏", "duō rén yóu xì", "Multijugador");
        putUi("menu.online", "Minecraft Realms", "Minecraft Realms", "Minecraft Realms");
        putUi("realms", "Realms", "Realms", "Realms");
        putUi("menu.disconnect", "断开连接", "duàn kāi lián jiē", "Desconectar");
        putUi("disconnect", "断开连接", "duàn kāi lián jiē", "Desconectar");
        putUi("menu.quit", "退出游戏", "tuì chū yóu xì", "Salir del juego");
        putUi("quit game", "退出游戏", "tuì chū yóu xì", "Salir del juego");
        putUi("menu.savingLevel", "保存并返回标题", "bǎo cún bìng fǎn huí biāo tí", "Guardar y volver al título");
        putUi("save and quit to title", "保存并返回标题", "bǎo cún bìng fǎn huí biāo tí", "Guardar y volver al título");
        putUi("menu.shareToLan", "对局域网开放", "duì jú yù wǎng kāi fàng", "Abrir en LAN");
        putUi("open to lan", "对局域网开放", "duì jú yù wǎng kāi fàng", "Abrir en LAN");
        putUi("gui.advancements", "进度", "jìn dù", "Progresos");
        putUi("advancements", "进度", "jìn dù", "Progresos");
        putUi("gui.stats", "统计信息", "tǒng jì xìn xī", "Estadísticas");
        putUi("statistics", "统计信息", "tǒng jì xìn xī", "Estadísticas");
        putUi("give feedback", "反馈", "fǎn kuì", "Enviar comentarios");
        putUi("report bugs", "报告漏洞", "bào gào lòu dòng", "Notificar errores");
        putUi("gui.done", "完成", "wán chéng", "Hecho");
        putUi("done", "完成", "wán chéng", "Hecho");
        putUi("terminado", "完成", "wán chéng", "Terminado");
        putUi("listo", "完成", "wán chéng", "Listo");
        putUi("gui.cancel", "取消", "qǔ xiāo", "Cancelar");
        putUi("cancel", "取消", "qǔ xiāo", "Cancelar");
        putUi("gui.back", "返回", "fǎn huí", "Atrás");
        putUi("back", "返回", "fǎn huí", "Atrás");
        putUi("gui.yes", "是", "shì", "Sí");
        putUi("yes", "是", "shì", "Sí");
        putUi("si", "是", "shì", "Sí");
        putUi("gui.no", "否", "fǒu", "No");
        putUi("no", "否", "fǒu", "No");
        putUi("options.on", "开启", "kāi qǐ", "Activado");
        putUi("on", "开启", "kāi qǐ", "Activado");
        putUi("options.off", "关闭", "guān bì", "Desactivado");
        putUi("off", "关闭", "guān bì", "Desactivado");
        putUi("options.guiScale.auto", "自动", "zì dòng", "Automático");
        putUi("auto", "自动", "zì dòng", "Automático");
        putUi("default", "默认", "mò rèn", "Por defecto");

        putUi("options.video", "视频设置", "shì pín shè zhì", "Gráficos");
        putUi("options.videoTitle", "视频设置", "shì pín shè zhì", "Gráficos");
        putUi("video settings", "视频设置", "shì pín shè zhì", "Gráficos");
        putUi("options.sounds", "音乐与声音", "yīn yuè yǔ shēng yīn", "Música y sonido");
        putUi("options.sounds.title", "音乐与声音", "yīn yuè yǔ shēng yīn", "Música y sonido");
        putUi("music & sounds", "音乐与声音", "yīn yuè yǔ shēng yīn", "Música y sonido");
        putUi("sonido", "声音", "shēng yīn", "Sonido");
        putUi("options.controls", "控制", "kòng zhì", "Controles");
        putUi("controls", "控制", "kòng zhì", "Controles");
        putUi("options.language", "语言", "yǔ yán", "Idioma");
        putUi("language", "语言", "yǔ yán", "Idioma");
        putUi("options.chat.title", "聊天设置", "liáo tiān shè zhì", "Opciones de chat");
        putUi("chat settings", "聊天设置", "liáo tiān shè zhì", "Opciones de chat");
        putUi("options.resourcepack", "资源包", "zī yuán bāo", "Paquetes de recursos");
        putUi("resource packs", "资源包", "zī yuán bāo", "Paquetes de recursos");
        putUi("options.accessibility.title", "无障碍设置", "wú zhàng ài shè zhì", "Accesibilidad");
        putUi("accessibility settings", "无障碍设置", "wú zhàng ài shè zhì", "Accesibilidad");
        putUi("options.skinCustomisation", "皮肤自定义", "pí fū zì dìng yì", "Aspecto");
        putUi("skin customization", "皮肤自定义", "pí fū zì dìng yì", "Aspecto");
        putUi("personalización de aspecto", "皮肤自定义", "pí fū zì dìng yì", "Aspecto");
        putUi("options.mouse_settings", "鼠标设置", "shǔ biāo shè zhì", "Opciones del ratón");
        putUi("mouse settings", "鼠标设置", "shǔ biāo shè zhì", "Opciones del ratón");
        putUi("options.keybinds", "按键绑定", "àn jiàn bǎng dìng", "Asignar teclas");
        putUi("key binds", "按键绑定", "àn jiàn bǎng dìng", "Asignar teclas");

        putUi("options.fov", "视场", "shì chǎng", "Campo visual");
        putUi("fov", "视场", "shì chǎng", "Campo visual");
        putUi("options.difficulty", "难度", "nán dù", "Dificultad");
        putUi("difficulty", "难度", "nán dù", "Dificultad");
        putUi("options.difficulty.peaceful", "和平", "hé píng", "Pacífico");
        putUi("peaceful", "和平", "hé píng", "Pacífico");
        putUi("options.difficulty.easy", "简单", "jiǎn dān", "Fácil");
        putUi("easy", "简单", "jiǎn dān", "Fácil");
        putUi("options.difficulty.normal", "普通", "pǔ tōng", "Normal");
        putUi("normal", "普通", "pǔ tōng", "Normal");
        putUi("options.difficulty.hard", "困难", "kùn nán", "Difícil");
        putUi("hard", "困难", "kùn nán", "Difícil");
        putUi("options.difficulty.hardcore", "极限", "jí xiàn", "Extremo");
        putUi("hardcore", "极限", "jí xiàn", "Extremo");
        putUi("selectWorld.gameMode", "游戏模式", "yóu xì mó shì", "Modo de juego");
        putUi("game mode", "游戏模式", "yóu xì mó shì", "Modo de juego");
        putUi("gameMode.survival", "生存模式", "shēng cún mó shì", "Supervivencia");
        putUi("survival", "生存模式", "shēng cún mó shì", "Supervivencia");
        putUi("gameMode.creative", "创造模式", "chuàng zào mó shì", "Creativo");
        putUi("creative", "创造模式", "chuàng zào mó shì", "Creativo");
        putUi("gameMode.adventure", "冒险模式", "mào xiǎn mó shì", "Aventura");
        putUi("adventure", "冒险模式", "mào xiǎn mó shì", "Aventura");
        putUi("gameMode.spectator", "旁观模式", "páng guān mó shì", "Espectador");
        putUi("spectator", "旁观模式", "páng guān mó shì", "Espectador");
        putUi("options.fullscreen", "全屏", "quán píng", "Pantalla completa");
        putUi("fullscreen", "全屏", "quán píng", "Pantalla completa");
        putUi("options.graphics", "图像", "tú xiàng", "Gráficos");
        putUi("graphics", "图像", "tú xiàng", "Gráficos");
        putUi("options.graphics.fast", "流畅", "liú chàng", "Rápidos");
        putUi("fast", "流畅", "liú chàng", "Rápidos");
        putUi("options.graphics.fancy", "高品质", "gāo pǐn zhì", "Detallados");
        putUi("fancy", "高品质", "gāo pǐn zhì", "Detallados");
        putUi("options.graphics.fabulous", "极高品质", "jí gāo pǐn zhì", "Fabulosos");
        putUi("fabulous", "极高品质", "jí gāo pǐn zhì", "Fabulosos");
        putUi("options.renderDistance", "渲染距离", "xuàn rǎn jù lí", "Distancia de renderizado");
        putUi("render distance", "渲染距离", "xuàn rǎn jù lí", "Distancia de renderizado");
        putUi("options.simulationDistance", "模拟距离", "mó nǐ jù lí", "Distancia de simulación");
        putUi("simulation distance", "模拟距离", "mó nǐ jù lí", "Distancia de simulación");
        putUi("options.framerateLimit", "最大帧率", "zuì dà zhēn lǜ", "Límite de fotogramas");
        putUi("max framerate", "最大帧率", "zuì dà zhēn lǜ", "Límite de fotogramas");
        putUi("options.vsync", "垂直同步", "chuí zhí tóng bù", "Sincronización vertical");
        putUi("vsync", "垂直同步", "chuí zhí tóng bù", "Sincronización vertical");
        putUi("options.gamma", "亮度", "liàng dù", "Brillo");
        putUi("brightness", "亮度", "liàng dù", "Brillo");
        putUi("options.gamma.min", "昏暗", "hūn àn", "Oscuro");
        putUi("moody", "昏暗", "hūn àn", "Oscuro");
        putUi("options.gamma.max", "明亮", "míng liàng", "Brillante");
        putUi("bright", "明亮", "míng liàng", "Brillante");
        putUi("options.guiScale", "界面尺寸", "jiè miàn chǐ cùn", "Escala de la interfaz");
        putUi("gui scale", "界面尺寸", "jiè miàn chǐ cùn", "Escala de la interfaz");
        putUi("options.ao", "平滑光照", "píng huá guāng zhào", "Iluminación suave");
        putUi("smooth lighting", "平滑光照", "píng huá guāng zhào", "Iluminación suave");
        putUi("options.viewBobbing", "视角摇晃", "shì jiǎo yáo huàng", "Balanceo de cámara");
        putUi("view bobbing", "视角摇晃", "shì jiǎo yáo huàng", "Balanceo de cámara");
        putUi("options.attackIndicator", "攻击指示器", "gōng jī zhǐ shì qì", "Indicador de ataque");
        putUi("attack indicator", "攻击指示器", "gōng jī zhǐ shì qì", "Indicador de ataque");
        putUi("options.renderClouds", "云", "yún", "Nubes");
        putUi("clouds", "云", "yún", "Nubes");
        putUi("options.particles", "颗粒效果", "kē lì xiào guǒ", "Partículas");
        putUi("particles", "颗粒效果", "kē lì xiào guǒ", "Partículas");
        putUi("options.entityShadows", "实体阴影", "shí tǐ yīn yǐng", "Sombras de entidades");
        putUi("entity shadows", "实体阴影", "shí tǐ yīn yǐng", "Sombras de entidades");
        putUi("soundCategory.master", "主音量", "zhǔ yīn liàng", "Volumen principal");
        putUi("master volume", "主音量", "zhǔ yīn liàng", "Volumen principal");
        putUi("soundCategory.music", "音乐", "yīn yuè", "Música");
        putUi("music", "音乐", "yīn yuè", "Música");
        putUi("soundCategory.record", "唱片机和音符盒", "chàng piān jī hé yīn fú hé", "Tocadiscos y bloques");
        putUi("jukebox/note blocks", "唱片机和音符盒", "chàng piān jī hé yīn fú hé", "Tocadiscos y bloques");
        putUi("soundCategory.weather", "天气", "tiān qì", "Clima");
        putUi("weather", "天气", "tiān qì", "Clima");
        putUi("soundCategory.block", "方块", "fāng kuài", "Bloques");
        putUi("blocks", "方块", "fāng kuài", "Bloques");
        putUi("soundCategory.hostile", "敌对生物", "dí duì shēng wù", "Criaturas hostiles");
        putUi("hostile creatures", "敌对生物", "dí duì shēng wù", "Criaturas hostiles");
        putUi("soundCategory.neutral", "友好生物", "yǒu hǎo shēng wù", "Criaturas pacíficas");
        putUi("friendly creatures", "友好生物", "yǒu hǎo shēng wù", "Criaturas pacíficas");
        putUi("soundCategory.player", "玩家", "wán jiā", "Jugadores");
        putUi("players", "玩家", "wán jiā", "Jugadores");
        putUi("soundCategory.ambient", "环境音效", "huán jìng yīn xiào", "Ambiente");
        putUi("ambient/environment", "环境音效", "huán jìng yīn xiào", "Ambiente");
        putUi("soundCategory.voice", "语音", "yǔ yīn", "Voz");
        putUi("voice/speech", "语音", "yǔ yīn", "Voz");

        putUi("selectWorld.select", "进入选中的世界", "jìn rù xuǎn zhòng de shì jiè", "Jugar al mundo seleccionado");
        putUi("play selected world", "进入选中的世界", "jìn rù xuǎn zhòng de shì jiè", "Jugar al mundo seleccionado");
        putUi("selectWorld.create", "创建新的世界", "chuàng jiàn xīn de shì jiè", "Crear un mundo nuevo");
        putUi("create new world", "创建新的世界", "chuàng jiàn xīn de shì jiè", "Crear un mundo nuevo");
        putUi("selectWorld.edit", "编辑", "biān jí", "Editar");
        putUi("edit", "编辑", "biān jí", "Editar");
        putUi("selectWorld.delete", "删除", "shān chú", "Eliminar");
        putUi("delete", "删除", "shān chú", "Eliminar");
        putUi("selectWorld.recreate", "重建", "chóng jiàn", "Recrear");
        putUi("re-create", "重建", "chóng jiàn", "Recrear");
        putUi("selectServer.direct", "直接连接", "zhí jiē lián jiē", "Conexión directa");
        putUi("direct connection", "直接连接", "zhí jiē lián jiē", "Conexión directa");
        putUi("selectServer.add", "添加服务器", "tiān jiā fú wù qì", "Añadir servidor");
        putUi("add server", "添加服务器", "tiān jiā fú wù qì", "Añadir servidor");
        putUi("selectServer.select", "加入服务器", "jiā rù fú wù qì", "Entrar al servidor");
        putUi("join server", "加入服务器", "jiā rù fú wù qì", "Entrar al servidor");
        putUi("selectServer.refresh", "刷新", "shuā xīn", "Actualizar");
        putUi("refresh", "刷新", "shuā xīn", "Actualizar");
        putUi("controls.reset", "重置", "zhòng zhì", "Restablecer");
        putUi("reset", "重置", "zhòng zhì", "Restablecer");
    }
}
