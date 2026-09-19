package com.khoand.mcgf;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;

/**
 * Module 2 — Companion entity.
 * Ke thua WolfEntity de tai su dung AI follow/attack + renderer so'i vanilla.
 * M3 se them chat LLM, M4 se them skill sinh ton.
 */
public class GfCompanionEntity extends WolfEntity {
    public GfCompanionEntity(EntityType<? extends WolfEntity> type, net.minecraft.world.World world) {
        super(type, world);
    }

    /** Chi so co ban: khoe nhu so'i da thuan (mau 40, chay nhanh, can 4 dmg). */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    /** true = di theo chu, false = ngoi yen (stay). */
    public void setFollowing(boolean following) {
        this.setSitting(!following);
    }

    public boolean isFollowing() {
        return !this.isSitting();
    }
}
