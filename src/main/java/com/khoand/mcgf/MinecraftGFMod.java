package com.khoand.mcgf;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module 1 — Core.
 * Nhiem vu: khoi dong mod, load config, dang ky lenh /gf.
 * Module 2+ se them: companion entity, follow AI, LLM chat, survival skills.
 */
public class MinecraftGFMod implements ModInitializer {
    public static final String MOD_ID = "mcgf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MCGF] Module 1 init: Minecraft 1.21.1 AI Companion core");
        GfConfig.load();
        GfCommands.register();
        LOGGER.info("[MCGF] Ten companion: {} | prefix chat: {}",
                GfConfig.get().companionName, GfConfig.get().chatPrefix);
    }
}
