package com.khoand.mcgf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Module 7 (client) — Ve companion bang model nguoi tay thuong (classic),
 * texture lay tu GfSkins (file/URL/mac dinh).
 * Module 8b — Hien vu khi tren tay + giap tren nguoi theo do dang mac.
 */
@Environment(EnvType.CLIENT)
public class GfCompanionRenderer
        extends BipedEntityRenderer<GfCompanionEntity, PlayerEntityModel<GfCompanionEntity>> {
    public GfCompanionRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
        BipedEntityModel<GfCompanionEntity> inner =
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR));
        BipedEntityModel<GfCompanionEntity> outer =
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR));
        this.addFeature(new ArmorFeatureRenderer<>(this, inner, outer, ctx.getModelManager()));
    }

    @Override
    public Identifier getTexture(GfCompanionEntity entity) {
        return GfSkins.getActive();
    }
}
