package com.khoand.mcgf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Module 4 — Sinh ton: danh quai, dao block, nhat do, cho an, tu hoi mau.
 * Tat ca chay tren logical server, ton trong luat survival
 * (tryBreakBlock roi do that, an thit that tu tui chu).
 */
public final class GfSurvival {
    /** Chong spam /gf mine: moi player 2 giay 1 phat (40 tick). */
    private static final Map<UUID, Long> MINE_COOLDOWN = new HashMap<>();

    private GfSurvival() {
    }

    // ---------- Chien dau ----------

    /** Ra lenh tan cong quai hostile gan nhat trong 16 block quanh pet. */
    public static int attack(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        Box box = pet.getBoundingBox().expand(16.0);
        List<HostileEntity> mobs = world.getEntitiesByClass(HostileEntity.class, box, HostileEntity::isAlive);
        HostileEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (HostileEntity m : mobs) {
            double d = m.squaredDistanceTo(pet);
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        if (best == null) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Quanh đây (16 block) không có quái nào cả!"), false);
            return 0;
        }
        pet.setSitting(false);
        pet.setTarget(best);
        pet.swingHand(Hand.MAIN_HAND);
        String targetName = best.getName().getString();
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                + "]§r Tấn công " + targetName + "! Bạn lùi lại để tui lo!"), false);
        return 1;
    }

    /** Dung danh, quay ve che do theo chu. */
    public static int stop(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        pet.setTarget(null);
        pet.setFollowing(true);
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Rõ, tui ngừng đánh!"), false);
        return 1;
    }

    // ---------- Dao block ----------

    /**
     * Pet dao giup block ma chu dang nhin (xa toi da 6 block).
     * Dung dung survival (roi do that, ton hao do ben tool).
     * Pet phai dung trong 8 block quanh block muc tieu.
     */
    public static int mine(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        HitResult hit = player.raycast(6.0, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Bạn nhìn vào block cần đào đi (trong 6 block) rồi gọi lại tui!"), false);
            return 0;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        if (pet.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64.0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Tui đứng xa quá, gọi /gf here cho tui lại gần rồi đào tiếp!"), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        Long last = MINE_COOLDOWN.get(player.getUuid());
        if (last != null && now - last < 40) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Từ từ, tui đang thở... (2 giây 1 phát)"), false);
            return 0;
        }
        MINE_COOLDOWN.put(player.getUuid(), now);

        pet.swingHand(Hand.MAIN_HAND);
        Vec3d c = Vec3d.ofCenter(pos);
        world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(pos)),
                c.x, c.y, c.z, 12, 0.3, 0.3, 0.3, 0.1);
        boolean ok = player.interactionManager.tryBreakBlock(pos);
        if (ok) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Đào xong!"), false);
            return 1;
        }
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                + "]§r Block này cứng quá (hoặc không đào được ở chế độ này)!"), false);
        return 0;
    }

    // ---------- Nhat do ----------

    /** Hut item + xp roi trong 10 block quanh chu vao tui chu. */
    public static int collect(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(10.0);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box,
                e -> !e.isRemoved() && !e.getStack().isEmpty());
        int moved = 0;
        for (ItemEntity ie : items) {
            if (moved >= 16) {
                break;
            }
            ItemStack copy = ie.getStack().copy();
            int before = copy.getCount();
            player.getInventory().insertStack(copy);
            if (copy.getCount() < before) {
                if (copy.isEmpty()) {
                    ie.discard();
                } else {
                    ie.setStack(copy);
                }
                moved++;
            }
        }
        List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, box,
                e -> !e.isRemoved());
        int xp = 0;
        for (ExperienceOrbEntity orb : orbs) {
            xp += orb.getExperienceAmount();
            orb.discard();
        }
        if (xp > 0) {
            player.addExperience(xp);
        }
        if (moved == 0 && xp == 0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Quanh đây (10 block) không có gì rơi cả!"), false);
            return 0;
        }
        String msg = "§b[" + GfConfig.get().companionName + "]§r Nhặt " + moved + " đống đồ"
                + (xp > 0 ? " + " + xp + " xp" : "") + " cho bạn rồi!";
        player.sendMessage(Text.literal(msg), false);
        return 1;
    }

    // ---------- Cho an + tu hoi mau ----------

    /** Lay 1 mieng thit tu tui chu cho pet an, hoi mau theo do chin/song. */
    public static int feed(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        if (pet.getHealth() >= pet.getMaxHealth()) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Tui đang đầy máu, để dành thịt đi!"), false);
            return 0;
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            float heal = meatHeal(stack.getItem());
            if (heal > 0.0F && !stack.isEmpty()) {
                stack.decrement(1);
                pet.heal(heal);
                ServerWorld world = player.getServerWorld();
                world.spawnParticles(ParticleTypes.HEART,
                        pet.getX(), pet.getY() + 1.2, pet.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
                player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                        + "]§r Ngon! Hồi " + (int) heal + " máu."), false);
                return 1;
            }
        }
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                + "]§r Tui đói... Bỏ thịt (sống/chín đều được) vào túi bạn rồi gõ /gf feed nha!"), false);
        return 0;
    }

    private static float meatHeal(Item item) {
        if (item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP) {
            return 8.0F;
        }
        if (item == Items.COOKED_MUTTON || item == Items.COOKED_CHICKEN
                || item == Items.COOKED_COD || item == Items.COOKED_SALMON) {
            return 6.0F;
        }
        if (item == Items.COOKED_RABBIT) {
            return 5.0F;
        }
        if (item == Items.ROTTEN_FLESH) {
            return 4.0F;
        }
        if (item == Items.BEEF || item == Items.PORKCHOP) {
            return 3.0F;
        }
        if (item == Items.MUTTON || item == Items.CHICKEN || item == Items.RABBIT
                || item == Items.COD || item == Items.SALMON) {
            return 2.0F;
        }
        return 0.0F;
    }

    /**
     * Tick moi 5 giay: ngoai giao tranh thi tu hoi 1 mau.
     * Goi tu server tick trong MinecraftGFMod.
     */
    public static void tickHeal(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null || pet.isRemoved()) {
            return;
        }
        if (pet.getHealth() >= pet.getMaxHealth()) {
            return;
        }
        if (pet.getTarget() != null || pet.getAttacker() != null) {
            return;
        }
        pet.heal(1.0F);
        if (pet.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    pet.getX(), pet.getY() + 1.0, pet.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
