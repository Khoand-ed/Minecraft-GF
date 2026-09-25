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
                reply(sender, "Hỏi tui gì đó sau '" + prefix + "' nha. VD: " + prefix + " creeper là gì?");
                return;
            }
            ask(sender, question);
        });
        MinecraftGFMod.LOGGER.info("[MCGF] Da bat nghe chat prefix '{}'", GfConfig.get().chatPrefix);
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
        return "Bạn là " + name + ", một chú sói bạn đồng hành trong Minecraft 1.21.1 (mod MCGF). "
                + "Tính cách vui vẻ, trung thành, nói tiếng Việt ngắn gọn dưới 200 ký tự, không dùng markdown. "
                + context + " "
                + "Các lệnh của bạn: /gf spawn (gọi ra), /gf follow (đi theo), /gf stay (ngồi yên), "
                + "/gf here (kéo về), /gf goto x y z (đến tọa độ), /gf dismiss (cho về), "
                + "/gf attack (đánh quái gần nhất), /gf stop (dừng đánh), "
                + "/gf mine (đào block đang nhìn), /gf collect (nhặt đồ rơi), /gf feed (cho ăn thịt). "
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
        String q = question.toLowerCase(Locale.ROOT);
        String name = GfConfig.get().companionName;

        if (contains(q, "chào", "chao", "hello", "hi ", "hí", "hey")) {
            return "Chào bạn! Tui là " + name + ", sói đồng hành của bạn đây. Gõ @gf + câu hỏi để trò chuyện nha!";
        }
        if (contains(q, "tên", "ten", "bạn là ai", "ban la ai", "là gì", "la gi")) {
            return "Tui là " + name + ", sói AI trong mod MCGF, biết đi theo, chiến đấu cùng bạn và trả lời câu hỏi!";
        }
        if (contains(q, "theo", "follow", "đi cùng", "di cung")) {
            return "Muốn tui đi theo thì gõ /gf follow nha. Muốn tui ngồi yên thì /gf stay!";
        }
        if (contains(q, "ngồi", "ngoi", "stay", "đứng yên", "dung yen", "dừng")) {
            return "Ok, gõ /gf stay là tui ngồi yên tại chỗ liền!";
        }
        if (contains(q, "đánh", "danh", "quái", "quai", "creeper", "zombie", "skeleton", "nhện", "nhen", "chiến", "attack")) {
            return "Gõ /gf attack là tui lao vào cắn quái gần nhất! Đánh xong gõ /gf stop để tui quay về. Tui cũng tự cắn đứa nào dám đánh bạn đó!";
        }
        if (contains(q, "đào", "dao", "mine", "cuốc", "cuoc", "kim cương", "kim cuong", "sắt", "sat")) {
            return "Nhìn vào block cần đào (trong 6 block) rồi gõ /gf mine, tui đào giúp! Nhớ gọi /gf here cho tui lại gần trước nha!";
        }
        if (contains(q, "ăn", "an", "feed", "máu", "mau", "hồi", "hoi", "đói", "doi", "thịt", "thit")) {
            return "Bỏ thịt vào túi bạn rồi gõ /gf feed là tui ăn và hồi máu! Ngoài giao tranh tui cũng tự hồi máu từ từ.";
        }
        if (contains(q, "nhặt", "nhat", "collect", "đồ rơi", "do roi", "rơi", "roi", "xp", "kinh nghiệm")) {
            return "Gõ /gf collect là tui hút đồ rơi + xp trong 10 block vào túi bạn liền!";
        }
        if (contains(q, "lệnh", "lenh", "giúp", "giup", "help", "dùng", "dung")) {
            return "Lệnh nè: /gf spawn, /gf follow, /gf stay, /gf here, /gf goto x y z, /gf dismiss, "
                    + "/gf attack, /gf stop, /gf mine, /gf collect, /gf feed. Chat thì gõ @gf + câu hỏi!";
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
