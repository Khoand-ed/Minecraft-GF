package com.khoand.mcgf;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * Module 9 — Chet va hoi sinh.
 * M9a: pet chet rot het kho + giap tai cho, bao toa do cho chu, ghi nhan lan nga.
 * M9b: /gf revive + /gf grave. M9c: tu hoi sinh theo timer.
 */
public final class GfRevive {
    /** Lan nga gan nhat theo tung chu (de M9b/M9c dung). */
    private static final Map<UUID, DeathInfo> DEATHS = new ConcurrentHashMap<>();

    private GfRevive() {
    }

    public static final class DeathInfo {
        public final String dim;
        public final BlockPos pos;
        public final long time;

        DeathInfo(String dim, BlockPos pos, long time) {
            this.dim = dim;
            this.pos = pos;
            this.time = time;
        }
    }

    public static DeathInfo getDeath(UUID owner) {
        return DEATHS.get(owner);
    }

    public static void clearDeath(UUID owner) {
        DEATHS.remove(owner);
    }

    /** Goi khi pet vua nga: rot do + nhan chu + ghi nhan. */
    public static void onDeath(GfCompanionEntity pet) {
        if (!(pet.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // Rot kho rieng + giap dang mac tai cho (survival that, chu tu nhat lai).
        Inventory bag = pet.getBag();
        for (int i = 0; i < bag.size(); i++) {
            ItemStack s = bag.getStack(i);
            if (!s.isEmpty()) {
                Block.dropStack(world, pet.getBlockPos(), s.copy());
            }
        }
        DefaultedList<ItemStack> gear = pet.getGear();
        for (int i = 0; i < gear.size(); i++) {
            ItemStack s = gear.get(i);
            if (!s.isEmpty()) {
                Block.dropStack(world, pet.getBlockPos(), s.copy());
            }
        }
        UUID ownerUuid = pet.getOwnerUuid();
        if (ownerUuid == null || pet.getServer() == null) {
            return;
        }
        BlockPos pos = pet.getBlockPos();
        String dim = world.getRegistryKey().getValue().toString();
        DEATHS.put(ownerUuid, new DeathInfo(dim, pos.toImmutable(), world.getTime()));
        ServerPlayerEntity owner = pet.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            String name = GfConfig.get().companionName;
            owner.sendMessage(Text.literal("§c[" + name + "]§r Tui ngã rồi... tại ("
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") " + dim
                    + ". Đồ tui rớt tại chỗ đó, bạn ra nhặt lại nha!"), false);
        }
    }
}
