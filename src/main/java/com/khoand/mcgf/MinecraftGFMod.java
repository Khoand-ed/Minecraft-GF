package com.khoand.mcgf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module 1 — Core. Module 2 — Companion follow. Module 3 — Chat AI.
 * Module 4 — Sinh ton.
 * Nhiem vu: khoi dong mod, load config, dang ky entity + lenh /gf + tick.
 * Module 5 se them: GUI config, persist, build release.
 */
public class MinecraftGFMod implements ModInitializer {
    public static final String MOD_ID = "mcgf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private int tickCounter = 0;
    private int healCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[MCGF] Module 1+2+3+4 init: Minecraft 1.21.1 AI Companion");
        GfConfig.load();
        GfEntities.register();
        GfCommands.register();
        GfBrain.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Moi 20 tick (~1 giay) keo companion bi lac ve gan chu.
            if (++tickCounter >= 20) {
                tickCounter = 0;
                server.getPlayerManager().getPlayerList().forEach(GfCompanionManager::tickFarTeleport);
            }
            // Moi 100 tick (~5 giay) tu hoi mau khi ngoai giao tranh.
            if (++healCounter >= 100) {
                healCounter = 0;
                server.getPlayerManager().getPlayerList().forEach(GfSurvival::tickHeal);
            }
        });
        LOGGER.info("[MCGF] Ten companion: {} | prefix chat: {}",
                GfConfig.get().companionName, GfConfig.get().chatPrefix);
    }
}
