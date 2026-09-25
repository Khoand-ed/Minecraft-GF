package com.khoand.mcgf;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Module 2+7 — Dang ky EntityType "mcgf:companion" cho MC 1.21.1.
 * M7: kich thuoc dang nguoi (0.6 x 1.8), id giu nguyen de pet cu tu chuyen.
 */
public final class GfEntities {
    public static EntityType<GfCompanionEntity> COMPANION;

    private GfEntities() {
    }

    public static void register() {
        COMPANION = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(MinecraftGFMod.MOD_ID, "companion"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GfCompanionEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
                        .trackRangeBlocks(10)
                        .build());

        FabricDefaultAttributeRegistry.register(COMPANION, GfCompanionEntity.createAttributes());
        MinecraftGFMod.LOGGER.info("[MCGF] Da dang ky entity mcgf:companion");
    }
}
