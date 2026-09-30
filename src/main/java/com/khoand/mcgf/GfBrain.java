package com.khoand.mcgf;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Module 3 — Chat AI. Module 5 — Persist tri nho.
 * Nghe chat server: tin nhan bat dau bang prefix (mac dinh "@gf")
 * se duoc tra loi. Co API key thi goi Gemini (async, khong lag server),
 * chua co key hoac loi mang thi tra loi offline tieng Viet.
 * Tri nho hoi-dap duoc luu xuong config/mcgf_history.json khi tat server
 * va nap lai khi mo server.
 */
public final class GfBrain {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mcgf-ai");
        t.setDaemon(true);
        return t;
    });
    /** Lich su hoi-dap theo tung player de AI nho context. */
    private static final Map<UUID, Deque<Msg>> HISTORY = new ConcurrentHashMap<>();

    private GfBrain() {
    }

    private static final class Msg {
        final String role; // "user" | "model"
        final String text;

        Msg(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString().trim();
            String prefix = GfConfig.get().chatPrefix;
            if (!text.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return;
            }
            String question = text.substring(prefix.length()).trim();
            if (question.isEmpty()) {
                reply(sender, emptyHint(prefix));
                return;
            }
            ask(sender, question);
        });
        MinecraftGFMod.LOGGER.info("[MCGF] Da bat nghe chat prefix '{}'", GfConfig.get().chatPrefix);
    }

    private static String emptyHint(String prefix) {
        switch (GfConfig.get().language.toLowerCase(Locale.ROOT)) {
            case "fr":
                return "Pose-moi une question après '" + prefix + "'. Ex : " + prefix + " c'est quoi un creeper ?";
            case "ja":
                return "「" + prefix + " ＜質問＞」と話しかけてね。例: " + prefix + " クリーパーってなに？";
            case "en":
                return "Ask me something after '" + prefix + "'. Ex: " + prefix + " what is a creeper?";
            default:
                return "Hỏi tui gì đó sau '" + prefix + "' nha. VD: " + prefix + " creeper là gì?";
        }
    }

    /** Diem vao chung cho chat prefix va lenh /gf ask. */
    public static void ask(ServerPlayerEntity player, String question) {
        pushHistory(player.getUuid(), "user", question);
        GfConfig.Data cfg = GfConfig.get();
        if (!cfg.aiEnabled || cfg.geminiApiKey == null || cfg.geminiApiKey.isBlank()) {
            String r = offlineReply(question);
            pushHistory(player.getUuid(), "model", r);
            reply(player, r);
            return;
        }
        MinecraftServer server = player.getServer();
        String key = cfg.geminiApiKey;
        String model = cfg.geminiModel;
        String name = cfg.companionName;
        Deque<Msg> history = new ArrayDeque<>(HISTORY.getOrDefault(player.getUuid(), new ArrayDeque<>()));
        String context = gameContext(player);
        UUID owner = player.getUuid();
        POOL.execute(() -> {
            String answer;
            try {
                answer = callGemini(key, model, name, context, history);
            } catch (Exception e) {
                MinecraftGFMod.LOGGER.warn("[MCGF] Gemini loi, dung offline: {}", e.toString());
                answer = offlineReply(question) + " (p/s: AI online lỗi nên tui trả lời chay đó!)";
            }
            pushHistory(owner, "model", answer);
            String out = answer;
            server.execute(() -> reply(server, out));
        });
    }

    public static void clearHistory(ServerPlayerEntity player) {
        HISTORY.remove(player.getUuid());
    }

    public static boolean isOnlineReady() {
        GfConfig.Data cfg = GfConfig.get();
        return cfg.aiEnabled && cfg.geminiApiKey != null && !cfg.geminiApiKey.isBlank();
    }

    // ---------- Gui tin ----------

    private static void reply(ServerPlayerEntity player, String text) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r " + text), false);
            return;
        }
        reply(server, text);
    }

    private static void reply(MinecraftServer server, String text) {
        server.getPlayerManager().broadcast(
                Text.literal("§b[" + GfConfig.get().companionName + "]§r " + text), false);
    }

    // ---------- Lich su ----------

    private static void pushHistory(UUID owner, String role, String text) {
        Deque<Msg> q = HISTORY.computeIfAbsent(owner, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(new Msg(role, text));
            int cap = Math.max(2, GfConfig.get().maxHistory * 2);
            while (q.size() > cap) {
                q.pollFirst();
            }
        }
    }

    // ---------- Module 5: persist tri nho ----------

    private static Path historyFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcgf_history.json");
    }

    /** Nap tri nho tu lan chay truoc (goi khi server started). */
    public static void loadHistory() {
        Path file = historyFile();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            int count = 0;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                UUID owner;
                try {
                    owner = UUID.fromString(e.getKey());
                } catch (IllegalArgumentException bad) {
                    continue;
                }
                Deque<Msg> q = new ArrayDeque<>();
                for (JsonElement je : e.getValue().getAsJsonArray()) {
                    JsonObject o = je.getAsJsonObject();
                    String role = o.has("role") ? o.get("role").getAsString() : "user";
                    String text = o.has("text") ? o.get("text").getAsString() : "";
                    if (role.equals("user") || role.equals("model")) {
                        q.addLast(new Msg(role, text));
                    }
                }
                if (!q.isEmpty()) {
                    HISTORY.put(owner, q);
                    count++;
                }
                if (count >= 20) {
                    break;
                }
            }
            MinecraftGFMod.LOGGER.info("[MCGF] Da nap tri nho AI cua {} player", count);
        } catch (Exception ex) {
            MinecraftGFMod.LOGGER.warn("[MCGF] Khong nap duoc tri nho AI: {}", ex.toString());
        }
    }

    /** Luu tri nho xuong dia (goi khi server stopping). */
    public static void saveHistory() {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, Deque<Msg>> e : HISTORY.entrySet()) {
                JsonArray arr = new JsonArray();
                synchronized (e.getValue()) {
                    for (Msg m : e.getValue()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("role", m.role);
                        o.addProperty("text", m.text);
                        arr.add(o);
                    }
                }
                root.add(e.getKey().toString(), arr);
            }
            try (Writer w = Files.newBufferedWriter(historyFile())) {
                PRETTY.toJson(root, w);
            }
            MinecraftGFMod.LOGGER.info("[MCGF] Da luu tri nho AI ({} player)", HISTORY.size());
        } catch (Exception ex) {
            MinecraftGFMod.LOGGER.warn("[MCGF] Khong luu duoc tri nho AI: {}", ex.toString());
        }
    }

    // ---------- Gemini ----------

    private static String callGemini(String key, String model, String name, String context, Deque<Msg> history)
            throws Exception {
        JsonObject root = new JsonObject();
        JsonObject sys = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text", systemPrompt(name, context));
        sysParts.add(sysText);
        sys.add("parts", sysParts);
        root.add("system_instruction", sys);

        JsonArray contents = new JsonArray();
        for (Msg m : history) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role);
            JsonArray parts = new JsonArray();
            JsonObject p = new JsonObject();
            p.addProperty("text", m.text);
            parts.add(p);
            o.add("parts", parts);
            contents.add(o);
        }
        root.add("contents", contents);

        JsonObject gen = new JsonObject();
        gen.addProperty("maxOutputTokens", 256);
        gen.addProperty("temperature", 0.7);
        root.add("generationConfig", gen);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + URLEncoder.encode(model, StandardCharsets.UTF_8)
                + ":generateContent?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(root)))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + res.statusCode() + ": " + truncate(res.body(), 200));
        }
        JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
        if (!body.has("candidates") || body.getAsJsonArray("candidates").isEmpty()) {
            throw new IllegalStateException(" Gemini tra ve rong: " + truncate(res.body(), 200));
        }
        JsonObject first = body.getAsJsonArray("candidates").get(0).getAsJsonObject();
        JsonArray parts = first.getAsJsonObject("content").getAsJsonArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i).getAsJsonObject().get("text").getAsString());
        }
        String answer = sb.toString().trim().replaceAll("\\s+", " ");
        if (answer.isEmpty()) {
            throw new IllegalStateException("Gemini tra ve text rong");
        }
        return truncate(answer, 300);
    }

    private static String systemPrompt(String name, String context) {
        String lang = GfConfig.get().language.toLowerCase(Locale.ROOT);
        String head;
        String style;
        if (lang.equals("fr")) {
            head = "Tu es " + name + ", un compagnon loyal dans Minecraft 1.21.1 (mod MCGF). ";
            style = "Sois joyeux et loyal, réponds en français, en moins de 200 caractères, sans markdown. ";
        } else if (lang.equals("ja")) {
            head = "あなたは" + name + "、Minecraft 1.21.1の頼れる仲間です(MCGF mod)。";
            style = "愉快で忠実に、日本語で200文字以内、markdownなしで短く返事してください。";
        } else if (lang.equals("en")) {
            head = "You are " + name + ", a loyal companion in Minecraft 1.21.1 (MCGF mod). ";
            style = "Be cheerful and loyal, reply in English, under 200 characters, no markdown. ";
        } else {
            head = "Bạn là " + name + ", một người bạn đồng hành trong Minecraft 1.21.1 (mod MCGF). ";
            style = "Tính cách vui vẻ, trung thành, nói tiếng Việt ngắn gọn dưới 200 ký tự, không dùng markdown. ";
        }
        return head + style
                + context + " "
                + "Các lệnh của bạn: /gf spawn (gọi ra), /gf follow (đi theo), /gf stay (ngồi yên), "
                + "/gf here (kéo về), /gf goto x y z (đến tọa độ), /gf dismiss (cho về), "
                + "/gf attack (đánh quái gần nhất), /gf stop (dừng đánh), "
                + "/gf mine (đào block đang nhìn), /gf collect (nhặt đồ rơi), /gf feed (cho ăn thịt), "
                + "/gf minevein (đào cả vỉa quặng), /gf chop (đốn cây), /gf farm (thu lúa + trồng lại), "
                + "/gf bag (xem kho pet), /gf give (lấy đồ từ kho pet), /gf deposit (cất vào rương gần nhất), "
                + "/gf equip (mặc vũ khí/giáp tốt nhất, dame và giáp tính thật), /gf unequip (cởi đồ), /gf gear (xem đồ). "
                + "Khi được hỏi cách chơi, hướng dẫn ngắn gọn đúng các lệnh này.";
    }

    private static String gameContext(ServerPlayerEntity p) {
        return "Người chơi " + p.getGameProfile().getName()
                + " đang ở (" + p.getBlockPos().getX() + ", " + p.getBlockPos().getY() + ", "
                + p.getBlockPos().getZ() + ") tại " + p.getServerWorld().getRegistryKey().getValue()
                + ", máu " + (int) p.getHealth() + "/" + (int) p.getMaxHealth()
                + ", độ đói " + p.getHungerManager().getFoodLevel() + "/20.";
    }

    private static String truncate(String s, int max) {
        if (s != null && s.length() > max) {
            return s.substring(0, max - 3) + "...";
        }
        return s;
    }

    // ---------- Fallback offline (tieng Viet) ----------

    static String offlineReply(String question) {
        switch (GfConfig.get().language.toLowerCase(Locale.ROOT)) {
            case "fr":
                return offlineReplyFr(question);
            case "ja":
                return offlineReplyJa(question);
            case "en":
                return offlineReplyEn(question);
            default:
                return offlineReplyVi(question);
        }
    }

    static String offlineReplyFr(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        String name = GfConfig.get().companionName;

        if (contains(q, "bonjour", "salut", "coucou", "hello", "bonsoir")) {
            return "Salut ! Je suis " + name + ", ton compagnon. Tape @gf + question pour discuter !";
        }
        if (contains(q, "ton nom", "qui es", "tu es quoi", "c'est quoi ton")) {
            return "Je suis " + name + ", un compagnon IA du mod MCGF. Je te suis, je combats avec toi et je réponds aux questions !";
        }
        if (contains(q, "suis", "suivre", "suivez", "viens", "suis-moi")) {
            return "Tape /gf follow et je te colle ! /gf stay pour rester immobile.";
        }
        if (contains(q, "reste", "assis", "attends", "immobile", "stop")) {
            return "Ok ! /gf stay et je reste ici même.";
        }
        if (contains(q, "attaque", "combat", "creeper", "zombie", "squelette", "araignée", "monstre", "tuer", "bats")) {
            return "Tape /gf attack et je charge le mob le plus proche ! /gf stop pour me rappeler. Je mords aussi quiconque te frappe !";
        }
        if (contains(q, "mine", "miner", "diamant", "fer", "pioche", "filon", "creuser", "minerai")) {
            return "Regarde un bloc et tape /gf mine ! Pour TOUT un filon : /gf minevein — le butin va dans mon sac, /gf give pour le prendre !";
        }
        if (contains(q, "manger", "nourrir", "viande", "faim", "vie", "soigner", "santé", "repas")) {
            return "Mets de la viande dans ton inventaire et tape /gf feed — je mange et je soigne ! Hors combat je régénère aussi doucement.";
        }
        if (contains(q, "ramasser", "récupérer", "prendre", "drop", "xp", "expérience", "sol")) {
            return "Tape /gf collect et j'aspire drops + xp à 10 blocs ! Mon butin à moi est dans mon sac — /gf bag, /gf give !";
        }
        if (contains(q, "sac", "donner", "donne", "coffre", "ranger", "range", "bois", "arbre", "couper", "ferme", "blé", "auto")) {
            return "J'ai mon sac perso de 9 slots ! /gf minevein pour miner, /gf chop pour abattre, /gf farm pour récolter (replante auto), /gf bag pour voir, /gf give pour prendre, /gf deposit pour ranger au coffre proche !";
        }
        if (contains(q, "équiper", "equiper", "armure", "arme", "épée", "porter", "mettre", "gear")) {
            return "Donne-moi armes/armures puis /gf equip — j'équipe le meilleur tout seul, dégâts et armure réels ! /gf gear pour voir, /gf unequip pour retirer !";
        }
        if (contains(q, "clé", "api", "gemini", "ligne", "intelligent", "en ligne")) {
            return "Demande à un OP de taper /gf apikey <clé Gemini gratuite> et je deviendrai bien plus malin ! Là je réponds hors-ligne.";
        }
        if (contains(q, "aide", "commande", "comment", "utiliser", "help")) {
            return "Commandes : /gf spawn, follow, stay, here, goto, dismiss, attack, stop, mine, collect, feed, minevein, chop, farm, bag, give, deposit, equip, unequip, gear. Discute avec @gf + question !";
        }
        if (contains(q, "merci")) {
            return "De rien ! Partir à l'aventure avec toi, c'est le meilleur !";
        }
        if (contains(q, "au revoir", "bye", "bonne nuit", "adieu")) {
            return "À plus ! Tape /gf spawn quand tu as besoin de moi !";
        }
        return "Hmm, colle (" + truncate(question, 60) + "). Parle-moi de Minecraft ou tape /gf help !";
    }

    static String offlineReplyEn(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        String name = GfConfig.get().companionName;

        if (contains(q, "hello", "hi ", "hey", "morning")) {
            return "Hey! I'm " + name + ", your companion. Type @gf + question to chat!";
        }
        if (contains(q, "your name", "who are you", "what are you")) {
            return "I'm " + name + ", an AI buddy in the MCGF mod. I follow you, fight with you and answer questions!";
        }
        if (contains(q, "follow", "come with", "follow me")) {
            return "Type /gf follow and I'll stick with you! /gf stay makes me sit still.";
        }
        if (contains(q, "stay", "sit", "wait", "stand still")) {
            return "Ok! /gf stay and I'll sit right here.";
        }
        if (contains(q, "attack", "fight", "creeper", "zombie", "skeleton", "spider", "combat", "mob", "kill")) {
            return "Type /gf attack and I'll charge the nearest mob! /gf stop to call me back. I also bite anyone who hits you!";
        }
        if (contains(q, "mine", "dig", "ore", "diamond", "iron", "pickaxe", "vein")) {
            return "Look at a block and type /gf mine! For a WHOLE vein use /gf minevein — loot goes to my bag, /gf give to take it!";
        }
        if (contains(q, "eat", "feed", "food", "meat", "hungry", "health", "heal", "hungry")) {
            return "Put meat in your inventory and type /gf feed — I'll eat and heal! I also regen slowly out of combat.";
        }
        if (contains(q, "collect", "pickup", "pick up", "drop", "xp", "experience")) {
            return "Type /gf collect and I'll pull drops + xp within 10 blocks to you! My own loot is in my bag — /gf bag, /gf give!";
        }
        if (contains(q, "bag", "give", "deposit", "chest", "store", "wood", "tree", "chop", "farm", "wheat", "auto")) {
            return "I have my own 9-slot bag! /gf minevein digs ore, /gf chop chops trees, /gf farm harvests (auto-replants), /gf bag views, /gf give takes, /gf deposit stores in the nearest chest!";
        }
        if (contains(q, "gear", "equip", "armor", "armour", "weapon", "sword", "wear")) {
            return "Give me weapons/armor, then /gf equip — I'll wear the best, damage and armor are real! /gf gear to view, /gf unequip to strip!";
        }
        if (contains(q, "key", "api", "gemini", "online", "smart")) {
            return "Ask an OP to run /gf apikey <free Gemini key> and I'll get way smarter! Right now I'm answering offline.";
        }
        if (contains(q, "help", "command", "how", "use")) {
            return "Commands: /gf spawn, follow, stay, here, goto, dismiss, attack, stop, mine, collect, feed, minevein, chop, farm, bag, give, deposit, equip, unequip, gear. Chat with @gf + question!";
        }
        if (contains(q, "thank")) {
            return "Anytime! Adventuring with you is the best!";
        }
        if (contains(q, "bye", "good night", "sleep")) {
            return "Bye! Type /gf spawn whenever you need me!";
        }
        return "Hmm, that's a tough one (" + truncate(question, 60) + "). Ask me about Minecraft or type /gf help!";
    }

    static String offlineReplyJa(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        String name = GfConfig.get().companionName;

        if (contains(q, "こんにちは", "こんにちわ", "ハロー", "やあ", "おはよう")) {
            return "やあ！僕は" + name + "、君の仲間だよ。「@gf + 質問」で話しかけてね！";
        }
        if (contains(q, "名前", "なまえ", "誰", "だれ")) {
            return "僕は" + name + "、MCGF modのAI仲間だよ。ついていくし、一緒に戦うし、質問にも答えるよ！";
        }
        if (contains(q, "ついて", "フォロー", "きて")) {
            return "/gf follow でついていくよ！/gf stay でその場で待つよ！";
        }
        if (contains(q, "待て", "まて", "すわり", "おすわり", "とまれ")) {
            return "わかった！/gf stay でここで待ってるね！";
        }
        if (contains(q, "攻撃", "こうげき", "戦", "クリーパー", "ゾンビ", "スケルトン", "モンスター", "クモ", "たたか")) {
            return "/gf attack で近くの敵に突撃するよ！/gf stop で戻るよ。君を攻撃する奴は僕がやっつける！";
        }
        if (contains(q, "掘る", "ほる", "ダイヤ", "鉱石", "こうせき", "鉄", "てつ", "つるはし", "マイン")) {
            return "ブロックを見て /gf mine！鉱脈ごとなら /gf minevein — 戦利品は僕のカバンに。/gf give で受け取って！";
        }
        if (contains(q, "食べる", "たべる", "餌", "えさ", "肉", "にく", "ご飯", "ごはん", "回復", "かいふく", "お腹", "おなか")) {
            return "君の持ち物に肉を入れて /gf feed！食べて回復するよ。戦闘以外では少しずつ自然回復するよ！";
        }
        if (contains(q, "集める", "あつめる", "拾う", "ひろう", "経験値", "けいけんち", "xp")) {
            return "/gf collect で10ブロック以内のドロップと経験値を集めるよ！僕の戦利品はカバンに — /gf bag、/gf give！";
        }
        if (contains(q, "カバン", "かばん", "渡す", "わたす", "チェスト", "預ける", "あずける", "木", "き", "畑", "はたけ", "自動", "じどう")) {
            return "僕専用の9スロットのカバンがあるよ！/gf minevein で採掘、/gf chop で伐採、/gf farm で収穫（植え直し付き）、/gf bag で確認、/gf give で受取、/gf deposit で近くのチェストに保管！";
        }
        if (contains(q, "装備", "そうび", "剣", "けん", "鎧", "よろい", "武器", "ぶき", "着る", "きる")) {
            return "武器・防具を渡して /gf equip！一番いいのを自動で装備するよ。攻撃力も防御力も本物！/gf gear で確認、/gf unequip で脱ぐよ！";
        }
        if (contains(q, "キー", "api", "gemini", "オンライン", "賢い", "かしこい")) {
            return "OPの人に /gf apikey <無料のGeminiキー> を入力してもらうと、もっと賢くなるよ！今はオフライン回答中！";
        }
        if (contains(q, "ヘルプ", "助けて", "たすけて", "コマンド", "使い方", "つかいかた")) {
            return "コマンド: /gf spawn, follow, stay, here, goto, dismiss, attack, stop, mine, collect, feed, minevein, chop, farm, bag, give, deposit, equip, unequip, gear。@gf + 質問でおしゃべり！";
        }
        if (contains(q, "ありがとう")) {
            return "どういたしまして！君と冒険するのが一番楽しいよ！";
        }
        if (contains(q, "バイバイ", "さようなら", "おやすみ", "じゃあね")) {
            return "またね！必要な時は /gf spawn で呼んで！";
        }
        return "うーん、難しい質問だ (" + truncate(question, 60) + ")。マイクラのことか /gf help を試してね！";
    }

    static String offlineReplyVi(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        String name = GfConfig.get().companionName;

        if (contains(q, "chào", "chao", "hello", "hi ", "hí", "hey")) {
            return "Chào bạn! Tui là " + name + ", bạn đồng hành của bạn đây. Gõ @gf + câu hỏi để trò chuyện nha!";
        }
        if (contains(q, "tên", "ten", "bạn là ai", "ban la ai", "là gì", "la gi")) {
            return "Tui là " + name + ", bạn AI trong mod MCGF, biết đi theo, chiến đấu cùng bạn và trả lời câu hỏi!";
        }
        if (contains(q, "theo", "follow", "đi cùng", "di cung")) {
            return "Muốn tui đi theo thì gõ /gf follow nha. Muốn tui ngồi yên thì /gf stay!";
        }
        if (contains(q, "ngồi", "ngoi", "stay", "đứng yên", "dung yen", "dừng")) {
            return "Ok, gõ /gf stay là tui ngồi yên tại chỗ liền!";
        }
        if (contains(q, "đánh", "danh", "quái", "quai", "creeper", "zombie", "skeleton", "nhện", "nhen", "chiến", "attack")) {
            return "Gõ /gf attack là tui lao vào đánh quái gần nhất! Đánh xong gõ /gf stop để tui quay về. Tui cũng tự đánh đứa nào dám đụng bạn đó!";
        }
        if (contains(q, "đào", "dao", "mine", "cuốc", "cuoc", "kim cương", "kim cuong", "sắt", "sat", "quặng", "quang", "vỉa", "via")) {
            return "Nhìn vào block rồi /gf mine để tui đào giúp! Muốn đào CẢ VỈA quặng thì /gf minevein — đồ tui giữ trong kho, xong gõ /gf give để lấy!";
        }
        if (contains(q, "ăn", "an", "feed", "máu", "mau", "hồi", "hoi", "đói", "doi", "thịt", "thit")) {
            return "Bỏ thịt vào túi bạn rồi gõ /gf feed là tui ăn và hồi máu! Ngoài giao tranh tui cũng tự hồi máu từ từ.";
        }
        if (contains(q, "giáp", "giap", "armor", "vũ khí", "vu khi", "weapon", "kiem", "kiếm", "sword", "trang bị", "trang bi", "equip", "gear", "mặc", "mac")) {
            return "Cho tui vũ khí/giáp vào kho tui (hoặc túi bạn) rồi gõ /gf equip — tui tự mặc đồ xịn nhất, dame và giáp tính thật! /gf gear để xem, /gf unequip để cởi!";
        }
        if (contains(q, "nhặt", "nhat", "collect", "đồ rơi", "do roi", "rơi", "roi", "xp", "kinh nghiệm")) {
            return "Gõ /gf collect là tui hút đồ rơi + xp trong 10 block vào túi bạn liền! Đồ tui đào/farm thì nằm trong kho tui — gõ /gf bag để xem, /gf give để lấy!";
        }
        if (contains(q, "kho", "túi pet", "tui pet", "give", "deposit", "rương", "ruong", "cất", "cat", "chặt", "chat cay", "đốn", "don", "farm", "trồng", "trong lua", "lúa")) {
            return "Tui có kho riêng 9 ô nè! /gf minevein đào quặng, /gf chop đốn cây, /gf farm thu lúa (tự trồng lại), /gf bag xem kho, /gf give lấy đồ, /gf deposit cất vào rương gần nhất!";
        }
        if (contains(q, "lệnh", "lenh", "giúp", "giup", "help", "dùng", "dung")) {
            return "Lệnh nè: /gf spawn, /gf follow, /gf stay, /gf here, /gf goto x y z, /gf dismiss, "
                    + "/gf attack, /gf stop, /gf mine, /gf collect, /gf feed, "
                    + "/gf minevein, /gf chop, /gf farm, /gf bag, /gf give, /gf deposit, "
                    + "/gf equip, /gf unequip, /gf gear. "
                    + "Chat thì gõ @gf + câu hỏi!";
        }
        if (contains(q, "cảm ơn", "cam on", "thanks", "thank")) {
            return "Không có chi! Được đi phiêu lưu cùng bạn là vui nhất rồi!";
        }
        if (contains(q, "tạm biệt", "tam biet", "bye", "ngủ", "ngu")) {
            return "Tạm biệt nha! Cần thì gọi /gf spawn, tui ra liền!";
        }
        if (contains(q, "key", "api", "gemini", "online", "thông minh")) {
            return "Muốn tui thông minh hẳn thì OP gõ /gf apikey <key Gemini miễn phí> nha, giờ tui đang trả lời chay đó!";
        }
        return "Hmm, câu này khó quá (" + truncate(question, 60) + "). Thử hỏi cách chơi Minecraft hoặc gõ /gf help nha!";
    }

    private static boolean contains(String q, String... keys) {
        for (String k : keys) {
            if (q.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
