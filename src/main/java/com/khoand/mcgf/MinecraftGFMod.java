package com.khoand.mcgf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module 1 — Core. Module 2 — Companion follow.
 * Nhiem vu: khoi dong mod, load config, dang ky entity + lenh /gf + tick.
 * Module 3+ se them: LLM chat, survival skills.
 */
public class MinecraftGFMod implements ModInitializer {
    public static final String MOD_ID = "mcgf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[MCGF] Module 1+2 init: Minecraft 1.21.1 AI Companion");
        GfConfig.load();
        GfEntities.register();
        GfCommands.register();
        // Moi 20 tick (~1 giay) keo companion bi lac ve gan chu.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < 20) {
                return;
            }
            tickCounter = 0;
            server.getPlayerManager().getPlayerList().forEach(GfCompanionManager::tickFarTeleport);
        });
        LOGGER.info("[MCGF] Ten companion: {} | prefix chat: {}",
                GfConfig.get().companionName, GfConfig.get().chatPrefix);
    }
}
