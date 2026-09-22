package com.khoand.mcgf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Module 1 — Config JSON don gian, khong phu thuoc LLM.
 * File: config/mcgf.json
 */
public final class GfConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data DATA = new Data();

    private GfConfig() {
    }

    public static Data get() {
        return DATA;
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcgf.json");
    }

    public static void load() {
        Path path = file();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                Data loaded = GSON.fromJson(r, Data.class);
                if (loaded != null) {
                    DATA = loaded;
                }
            } catch (IOException e) {
                MinecraftGFMod.LOGGER.warn("[MCGF] Khong doc duoc config, dung mac dinh: {}", e.getMessage());
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(DATA, w);
        } catch (IOException e) {
            MinecraftGFMod.LOGGER.warn("[MCGF] Khong ghi duoc config: {}", e.getMessage());
        }
    }

    /** POJO config — Module 3 se dung geminiApiKey/geminiModel. */
    public static class Data {
        public String companionName = "GF";
        public String chatPrefix = "@gf";
        public double followDistance = 3.0;
        /** Qua khoang cach nay (block) thi tu dich chuyen ve canh chu. */
        public double teleportDistance = 24.0;
        public boolean replyInVietnamese = true;
        /** Module 3 — bat/tat AI. Tat thi chi tra loi offline. */
        public boolean aiEnabled = true;
        /** So cap hoi-dap gan nhat giu lai lam context cho AI. */
        public int maxHistory = 8;
        public String geminiApiKey = "";
        public String geminiModel = "gemini-2.0-flash";
    }
}
