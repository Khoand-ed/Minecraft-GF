package com.khoand.mcgf;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
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
    /** Cooldown hoi sinh: 60 giay (1200 tick). */
    private static final Map<UUID, Long> REVIVE_COOLDOWN = new ConcurrentHashMap<>();

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

    /** Hoi sinh pet canh player. Do rot van nam o cho nga (chu tu nhat lai). */
    public static int revive(ServerPlayerEntity player) {
        GfCompanionEntity alive = GfCompanionManager.findOwned(player);
        if (alive != null && !alive.isRemoved()) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Tui vẫn sống nhăn đây, khỏi hồi sinh!"), false);
            return 0;
        }
        long now = player.getServerWorld().getTime();
        Long last = REVIVE_COOLDOWN.get(player.getUuid());
        if (last != null && now - last < 1200) {
            long wait = (1200 - (now - last) + 19) / 20;
            player.sendMessage(Text.literal("§b[MCGF]§r Hồi sinh đang hồi (" + wait
                    + "s nữa). Bình tĩnh!"), false);
            return 0;
        }
        GfCompanionEntity pet = GfCompanionManager.summon(player);
        if (pet == null) {
            return 0;
        }
        REVIVE_COOLDOWN.put(player.getUuid(), now);
        String name = GfConfig.get().companionName;
        DeathInfo d = DEATHS.get(player.getUuid());
        DEATHS.remove(player.getUuid());
        player.sendMessage(Text.literal("§b[" + name + "]§r Tui sống lại rồi! "
                + (d != null ? "Đồ tui vẫn nằm ở chỗ ngã (" + d.pos.getX() + ", " + d.pos.getY()
                        + ", " + d.pos.getZ() + "), ra nhặt lại nha!" : "Chiến tiếp thôi!")),
                false);
        return 1;
    }

    /** Xem cho nga gan nhat. */
    public static int grave(ServerPlayerEntity player) {
        DeathInfo d = DEATHS.get(player.getUuid());
        if (d == null) {
            player.sendMessage(Text.literal("§b[MCGF]§r Không có dữ liệu ngã. "
                    + "Pet còn sống thì dùng /gf here để kéo về!"), false);
            return 0;
        }
        player.sendMessage(Text.literal("§b[MCGF]§r Pet ngã tại (" + d.pos.getX() + ", "
                + d.pos.getY() + ", " + d.pos.getZ() + ") " + d.dim
                + ". Đồ rớt quanh đó — /gf revive để hồi sinh pet mới!"), false);
        return 1;
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

    /**
     * Module 9c — Tu hoi sinh sau autoReviveMinutes (0 = tat).
     * Goi moi 20 tick tu MinecraftGFMod.
     */
    public static void tickAuto(MinecraftServer server) {
        if (DEATHS.isEmpty()) {
            return;
        }
        int minutes = GfConfig.get().autoReviveMinutes;
        if (minutes <= 0) {
            return;
        }
        long need = (long) minutes * 1200L;
        long now = server.getOverworld().getTime();
        for (var it = DEATHS.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) {
                continue;
            }
            GfCompanionEntity alive = GfCompanionManager.findOwned(player);
            if (alive != null && !alive.isRemoved()) {
                it.remove();
                continue;
            }
            if (now - e.getValue().time < need) {
                continue;
            }
            it.remove();
            REVIVE_COOLDOWN.remove(e.getKey());
            GfCompanionEntity pet = GfCompanionManager.summon(player);
            if (pet != null) {
                player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                        + "]§r Tui tự sống lại rồi nè! Đồ cũ vẫn nằm ở chỗ ngã, ra nhặt nha!"), false);
            }
        }
    }
}
