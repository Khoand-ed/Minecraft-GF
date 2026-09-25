package com.khoand.mcgf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Module 7 — Client: renderer nguoi + nap skin custom.
 * File nay PHAI nam o src/client (Loom split env), khong duoc o src/main.
 */
public class GfClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GfSkins.init();
        EntityRendererRegistry.register(GfEntities.COMPANION, GfCompanionRenderer::new);
    }
}
