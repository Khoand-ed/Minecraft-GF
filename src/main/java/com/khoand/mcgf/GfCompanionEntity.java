package com.khoand.mcgf;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Module 7 — Companion DANG NGUOI (thay the dang soi tu Module 2).
 * Ke thua TameableEntity: giu nguyen follow/chu/bao ve chu + kho rieng.
 * Model nguoi + skin do GfCompanionRenderer (src/client) ve.
 * Cung EntityType id "mcgf:companion" + key NBT "MCGFBag" nhu cu
 * nen pet soi cu tu chuyen thanh nguoi (giu kho do).
 */
public class GfCompanionEntity extends TameableEntity {
    /** Kho rieng 9 o: /gf bag xem, /gf give lay, /gf deposit cat ruong. */
    private final SimpleInventory bag = new SimpleInventory(9);
    /**
     * Module 8a — Trang bi: 0 tay chinh, 1 mu, 2 ao, 3 quan, 4 giay.
     * Giap tinh that qua getEquippedStack, dame tay tinh qua attribute.
     */
    private final DefaultedList<ItemStack> gear = DefaultedList.ofSize(5, ItemStack.EMPTY);

    public GfCompanionEntity(EntityType<? extends TameableEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.0, 6.0F, 2.0F));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, HostileEntity.class, false));
    }

    /** Chi so dang nguoi: mau 40, toc do + danh nhu chien binh. */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    /** true = di theo chu, false = dung yen (stay, hien pose sneak). */
    public void setFollowing(boolean following) {
        this.setSitting(!following);
        this.setSneaking(!following);
    }

    public boolean isFollowing() {
        return !this.isSitting();
    }

    /** Khong nhan giong (can thiep breed thi tra ve null). */
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    public Inventory getBag() {
        return this.bag;
    }

    /** Trang bi mac tren nguoi (0 tay, 1 mu, 2 ao, 3 quan, 4 giay). */
    public DefaultedList<ItemStack> getGear() {
        return this.gear;
    }

    /** Ghi de de giap tinh that tu do dang mac (LivingEntity.getArmor tu goi ham nay). */
    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        switch (slot) {
            case MAINHAND:
                return this.gear.get(0);
            case HEAD:
                return this.gear.get(1);
            case CHEST:
                return this.gear.get(2);
            case LEGS:
                return this.gear.get(3);
            case FEET:
                return this.gear.get(4);
            default:
                return ItemStack.EMPTY;
        }
    }

    /** Cap nhat dame danh theo vu khi dang cam (tay khong = 4). */
    public void refreshAttackDamage() {
        var attr = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            attr.setBaseValue(Math.max(4.0, GfGear.weaponDamage(this.gear.get(0).getItem())));
        }
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
        Inventories.writeNbt(bagNbt, this.bag.getHeldStacks(), this.getWorld().getRegistryManager());
        nbt.put("MCGFBag", bagNbt);
        NbtCompound gearNbt = new NbtCompound();
        Inventories.writeNbt(gearNbt, this.gear, this.getWorld().getRegistryManager());
        nbt.put("MCGFGear", gearNbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bag.clear();
        if (nbt.contains("MCGFBag", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("MCGFBag"), this.bag.getHeldStacks(),
                    this.getWorld().getRegistryManager());
        }
        for (int i = 0; i < this.gear.size(); i++) {
            this.gear.set(i, ItemStack.EMPTY);
        }
        if (nbt.contains("MCGFGear", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("MCGFGear"), this.gear,
                    this.getWorld().getRegistryManager());
        }
        this.refreshAttackDamage();
    }
}
