package com.khoand.mcgf;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Module 6 — Helper nhet do vao bat ky Inventory nao (kho pet, ruong, thung).
 * Dung API giao dien co ban (getStack/setStack) de tuong thich moi loai kho.
 */
public final class GfInv {
    private GfInv() {
    }

    /**
     * Nhet stack vao kho. Tra ve so luong CON THUA (0 = vua het).
     * Stack truyen vao bi tru dan theo so da nhet duoc.
     */
    public static int insert(Inventory inv, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        // 1. Don vao stack cung loai co san.
        for (int i = 0; i < inv.size() && !stack.isEmpty(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && ItemStack.areItemsAndComponentsEqual(s, stack)
                    && s.getCount() < s.getMaxCount()) {
                int move = Math.min(stack.getCount(), s.getMaxCount() - s.getCount());
                s.increment(move);
                stack.decrement(move);
            }
        }
        // 2. O trong.
        for (int i = 0; i < inv.size() && !stack.isEmpty(); i++) {
            if (inv.getStack(i).isEmpty()) {
                int move = Math.min(stack.getCount(), stack.getMaxCount());
                ItemStack put = stack.copy();
                put.setCount(move);
                inv.setStack(i, put);
                stack.decrement(move);
            }
        }
        inv.markDirty();
        return stack.getCount();
    }

    /** Tong so item (dem vien) trong kho. */
    public static int countItems(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            n += inv.getStack(i).getCount();
        }
        return n;
    }
}
