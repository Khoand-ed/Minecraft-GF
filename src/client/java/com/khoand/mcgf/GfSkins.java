package com.khoand.mcgf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

/**
 * Module 7 (client) — Skin companion dang nguoi, tay thuong (classic).
 * Thu tu uu tien: file config/mcgf_skin.png (PNG 64x64) →
 * skinUrl trong config → skin mac dinh trong assets.
 */
@Environment(EnvType.CLIENT)
public final class GfSkins {
    public static final Identifier DEFAULT =
            Identifier.of(MinecraftGFMod.MOD_ID, "textures/entity/companion.png");
    private static final Identifier CUSTOM =
            Identifier.of(MinecraftGFMod.MOD_ID, "custom_skin");
    private static volatile Identifier active = DEFAULT;

    private GfSkins() {
    }

    public static Identifier getActive() {
        return active;
    }

    public static void init() {
        // 1. File local.
        Path file = FabricLoader.getInstance().getConfigDir().resolve(GfConfig.get().skinFile);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                if (apply(NativeImage.read(in))) {
                    MinecraftGFMod.LOGGER.info("[MCGF] Dung skin tu file {}", file.getFileName());
                    return;
                }
            } catch (Exception e) {
                MinecraftGFMod.LOGGER.warn("[MCGF] Khong doc duoc skin file: {}", e.toString());
            }
        }
        // 2. URL.
        String url = GfConfig.get().skinUrl;
        if (url != null && !url.isBlank()) {
            fetchUrl(url.trim());
        }
    }

    private static void fetchUrl(String url) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(res -> {
                if (res.statusCode() != 200) {
                    MinecraftGFMod.LOGGER.warn("[MCGF] Tai skin URL loi HTTP {}", res.statusCode());
                    return;
                }
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        if (apply(NativeImage.read(new ByteArrayInputStream(res.body())))) {
                            MinecraftGFMod.LOGGER.info("[MCGF] Dung skin tu URL");
                        }
                    } catch (Exception e) {
                        MinecraftGFMod.LOGGER.warn("[MCGF] Skin URL khong hop le: {}", e.toString());
                    }
                });
            });
        } catch (Exception e) {
            MinecraftGFMod.LOGGER.warn("[MCGF] URL skin sai: {}", e.toString());
        }
    }

    /** Nap anh lam texture. Tra ve false neu khong phai PNG skin 64x64/64x32. */
    private static boolean apply(NativeImage img) {
        if (img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
            MinecraftGFMod.LOGGER.warn("[MCGF] Skin phai la PNG 64x64 (duoc {})",
                    img.getWidth() + "x" + img.getHeight());
            img.close();
            return false;
        }
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(CUSTOM, new DynamicTexture(img));
        active = CUSTOM;
        return true;
    }
}
