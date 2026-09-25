package com.khoand.mcgf;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * Module 2 — Companion entity.
 * Ke thua WolfEntity de tai su dung AI follow/attack + renderer so'i vanilla.
 * Module 6 — Kho do rieng 9 o, luu theo entity (NBT "MCGFBag").
 * (Module 7 se doi sang dang nguoi nhung giu nguyen kho + NBT key.)
 */
public class GfCompanionEntity extends WolfEntity {
    /** Kho rieng: do pet dao/farm duoc hut vao day, chu lay lai bang /gf give. */
    private final SimpleInventory bag = new SimpleInventory(9);

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

    public Inventory getBag() {
        return this.bag;
    }

    /** Tong so item trong kho (dem theo vien). */
    public int bagItemCount() {
        int n = 0;
        for (int i = 0; i < this.bag.size(); i++) {
            n += this.bag.getStack(i).getCount();
        }
        return n;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtCompound bagNbt = new NbtCompound();
        Inventories.writeNbt(bagNbt, this.bag.getHeldStacks());
        nbt.put("MCGFBag", bagNbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bag.clear();
        if (nbt.contains("MCGFBag", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("MCGFBag"), this.bag.getHeldStacks());
        }
    }
}
