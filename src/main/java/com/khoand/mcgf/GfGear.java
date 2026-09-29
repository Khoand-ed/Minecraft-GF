package com.khoand.mcgf;

import net.minecraft.block.Block;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Module 8a — Trang bi cho pet: vu khi cam tay + 4 mon giap.
 * Dame/giap deu tinh THAT (attribute + getEquippedStack), luu NBT "MCGFGear".
 * Hinh anh hien tren nguoi o Module 8b.
 */
public final class GfGear {
    private GfGear() {
    }

    /** Slot gear tuong ung EquipmentSlot (bo OFFHAND). */
    public static int slotIndex(EquipmentSlot slot) {
        switch (slot) {
            case MAINHAND:
                return 0;
            case HEAD:
                return 1;
            case CHEST:
                return 2;
            case LEGS:
                return 3;
            case FEET:
                return 4;
            default:
                return -1;
        }
    }

    /** Dame danh cua vu khi (tay khong = 4). Kiem > riu > tool khac. */
    public static double weaponDamage(Item item) {
        if (item == Items.NETHERITE_SWORD) {
            return 8.0;
        }
        if (item == Items.DIAMOND_SWORD) {
            return 7.0;
        }
        if (item == Items.IRON_SWORD) {
            return 6.0;
        }
        if (item == Items.STONE_SWORD || item == Items.GOLDEN_SWORD) {
            return 5.0;
        }
        if (item == Items.WOODEN_SWORD) {
            return 4.0;
        }
        if (item == Items.NETHERITE_AXE) {
            return 10.0;
        }
        if (item == Items.DIAMOND_AXE) {
            return 9.0;
        }
        if (item == Items.IRON_AXE) {
            return 8.0;
        }
        if (item == Items.STONE_AXE || item == Items.GOLDEN_AXE) {
            return 7.0;
        }
        if (item == Items.WOODEN_AXE) {
            return 6.0;
        }
        if (item == Items.TRIDENT) {
            return 8.0;
        }
        if (item == Items.MACE) {
            return 6.0;
        }
        if (item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_SHOVEL
                || item == Items.DIAMOND_PICKAXE || item == Items.DIAMOND_SHOVEL) {
            return 5.0;
        }
        if (item == Items.IRON_PICKAXE || item == Items.IRON_SHOVEL
                || item == Items.STONE_PICKAXE || item == Items.STONE_SHOVEL
                || item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_SHOVEL) {
            return 4.0;
        }
        if (item == Items.WOODEN_PICKAXE || item == Items.WOODEN_SHOVEL) {
            return 3.0;
        }
        return 0.0;
    }

    /** Diem giap cua 1 mon (0 = khong phai giap). */
    public static int armorValue(Item item) {
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE) {
            return item == Items.NETHERITE_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE ? 8 : 3;
        }
        if (item == Items.NETHERITE_LEGGINGS || item == Items.DIAMOND_LEGGINGS) {
            return 6;
        }
        if (item == Items.NETHERITE_BOOTS || item == Items.DIAMOND_BOOTS) {
            return 3;
        }
        if (item == Items.IRON_HELMET || item == Items.CHAINMAIL_HELMET
                || item == Items.GOLDEN_HELMET || item == Items.TURTLE_HELMET) {
            return 2;
        }
        if (item == Items.IRON_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE) {
            return item == Items.GOLDEN_CHESTPLATE ? 5 : item == Items.IRON_CHESTPLATE ? 6 : 5;
        }
        if (item == Items.IRON_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS
                || item == Items.GOLDEN_LEGGINGS) {
            return item == Items.IRON_LEGGINGS ? 5 : 4;
        }
        if (item == Items.IRON_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.GOLDEN_BOOTS) {
            return 1;
        }
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_BOOTS) {
            return 1;
        }
        if (item == Items.LEATHER_CHESTPLATE) {
            return 3;
        }
        if (item == Items.LEATHER_LEGGINGS) {
            return 2;
        }
        return 0;
    }

    private static String slotName(int i) {
        switch (i) {
            case 0:
                return "Tay";
            case 1:
                return "Mũ";
            case 2:
                return "Áo";
            case 3:
                return "Quần";
            case 4:
                return "Giày";
            default:
                return "?";
        }
    }

    /**
     * Mac do tot nhat: quet kho pet truoc roi den tui chu.
     * Vu khi: dame cao nhat. Giap: tung slot lay diem cao nhat.
     */
    public static int equipBest(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        int changed = 0;
        // Tay: vu khi dame cao nhat.
        changed += equipWeaponFrom(player, pet, pet.getBag());
        changed += equipWeaponFrom(player, pet, player.getInventory());
        // Giap tung slot.
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            changed += equipArmorFrom(player, pet, slot, pet.getBag());
            changed += equipArmorFrom(player, pet, slot, player.getInventory());
        }
        pet.refreshAttackDamage();
        if (changed == 0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Không thấy vũ khí/giáp nào tốt hơn trong kho tui và túi bạn!"), false);
            return 0;
        }
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                + "]§r Mặc xong " + changed + " món! Dame tay " + pet.getAttributeValue(
                        net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)
                + ", giáp " + pet.getArmor() + ". Gõ /gf gear để xem!"), false);
        return 1;
    }

    private static int equipWeaponFrom(ServerPlayerEntity player, GfCompanionEntity pet, Inventory src) {
        double cur = weaponDamage(pet.getGear().get(0).getItem());
        int bestSlot = -1;
        double bestVal = cur;
        for (int i = 0; i < src.size(); i++) {
            double v = weaponDamage(src.getStack(i).getItem());
            if (v > bestVal) {
                bestVal = v;
                bestSlot = i;
            }
        }
        if (bestSlot < 0) {
            return 0;
        }
        takeOneToGear(player, pet, src, bestSlot, 0);
        return 1;
    }

    private static int equipArmorFrom(ServerPlayerEntity player, GfCompanionEntity pet,
            EquipmentSlot slot, Inventory src) {
        int gi = slotIndex(slot);
        int cur = armorValue(pet.getGear().get(gi).getItem());
        int bestSlot = -1;
        int bestVal = cur;
        for (int i = 0; i < src.size(); i++) {
            ItemStack s = src.getStack(i);
            if (s.isEmpty() || !(s.getItem() instanceof ArmorItem armor)) {
                continue;
            }
            if (armor.getSlotType() != slot) {
                continue;
            }
            int v = armorValue(s.getItem());
            if (v > bestVal) {
                bestVal = v;
                bestSlot = i;
            }
        }
        if (bestSlot < 0) {
            return 0;
        }
        takeOneToGear(player, pet, src, bestSlot, gi);
        return 1;
    }

    /** Lay 1 cai tu kho nguon sang slot gear, do cu tra ve kho pet. */
    private static void takeOneToGear(ServerPlayerEntity player, GfCompanionEntity pet,
            Inventory src, int srcSlot, int gearIndex) {
        ItemStack take = src.getStack(srcSlot).copy();
        take.setCount(1);
        src.getStack(srcSlot).decrement(1);
        if (src instanceof net.minecraft.entity.player.PlayerInventory) {
            src.markDirty();
        }
        ItemStack old = pet.getGear().get(gearIndex);
        pet.getGear().set(gearIndex, take);
        if (!old.isEmpty()) {
            ItemStack back = old.copy();
            int left = GfInv.insert(pet.getBag(), back);
            if (left > 0) {
                back.setCount(left);
                player.getInventory().insertStack(back);
                if (!back.isEmpty()) {
                    Block.dropStack(player.getServerWorld(), player.getBlockPos(), back);
                }
            }
        }
    }

    /** Coi het do ve tui chu. */
    public static int unequipAll(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        int n = 0;
        ServerWorld world = player.getServerWorld();
        for (int i = 0; i < pet.getGear().size(); i++) {
            ItemStack s = pet.getGear().get(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack copy = s.copy();
            player.getInventory().insertStack(copy);
            if (!copy.isEmpty()) {
                Block.dropStack(world, player.getBlockPos(), copy);
            }
            pet.getGear().set(i, ItemStack.EMPTY);
            n++;
        }
        pet.refreshAttackDamage();
        if (n == 0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Tui đang mình không!"), false);
            return 0;
        }
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Cởi " + n + " món trả bạn!"), false);
        return 1;
    }

    /** Xem do dang mac. */
    public static int showGear(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("§b[MCGF] Đồ đang mặc:§r");
        for (int i = 0; i < pet.getGear().size(); i++) {
            ItemStack s = pet.getGear().get(i);
            sb.append("\n§7- ").append(slotName(i)).append(": §e")
                    .append(s.isEmpty() ? "(trống)" : s.getName().getString());
        }
        sb.append("\n§7Dame tay: §e").append(pet.getAttributeValue(
                net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE));
        sb.append("§7, giáp: §e").append(pet.getArmor());
        player.sendMessage(Text.literal(sb.toString()), false);
        return 1;
    }
}
