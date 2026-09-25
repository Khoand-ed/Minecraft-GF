package com.khoand.mcgf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Module 7 (client) — Ve companion bang model nguoi tay thuong (classic),
 * texture lay tu GfSkins (file/URL/mac dinh).
 */
@Environment(EnvType.CLIENT)
public class GfCompanionRenderer
        extends BipedEntityRenderer<GfCompanionEntity, PlayerEntityModel<GfCompanionEntity>> {
    public GfCompanionRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(GfCompanionEntity entity) {
        return GfSkins.getActive();
    }
}
