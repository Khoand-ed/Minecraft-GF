package com.khoand.mcgf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.WolfEntityRenderer;

/**
 * Module 2 — Client: dung lai renderer so'i vanilla cho companion
 * (GfCompanionEntity ke thua WolfEntity nen tuong thich).
 * File nay PHAI nam o src/client (Loom split env), khong duoc o src/main.
 */
public class GfClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GfEntities.COMPANION, WolfEntityRenderer::new);
    }
}
