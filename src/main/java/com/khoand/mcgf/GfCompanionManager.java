package com.khoand.mcgf;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Module 2 — Quan ly companion theo tung player: spawn, follow, stay,
 * goto, here (keo ve), dismiss. Chay tren logical server.
 */
public final class GfCompanionManager {
    private GfCompanionManager() {
    }

    /** Tim companion gan nhat ma player nay so huu (ban kinh 128 block). */
    public static GfCompanionEntity findOwned(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID owner = player.getUuid();
        List<GfCompanionEntity> list = world.getEntitiesByClass(
                GfCompanionEntity.class,
                player.getBoundingBox().expand(128.0),
                e -> e.isTamed() && owner.equals(e.getOwnerUuid()));
        if (list.isEmpty()) {
            return null;
        }
        GfCompanionEntity best = list.get(0);
        double bestDist = best.squaredDistanceTo(player);
        for (GfCompanionEntity e : list) {
            double d = e.squaredDistanceTo(player);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /** Spawn companion moi tai vi tri player; neu da co thi bao lai. */
    public static int spawn(ServerPlayerEntity player) {
        GfCompanionEntity old = findOwned(player);
        if (old != null && !old.isRemoved()) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Toi dang o day roi! Dung /gf here de keo toi ve."), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        GfCompanionEntity pet = new GfCompanionEntity(GfEntities.COMPANION, world);
        pet.refreshPositionAndAngles(player.getX() + 1.0, player.getY(), player.getZ() + 1.0,
                player.getYaw(), 0.0F);
        pet.setTamed(true, true);
        pet.setOwner(player);
        String name = GfConfig.get().companionName;
        pet.setCustomName(Text.literal(name));
        pet.setCustomNameVisible(true);
        pet.setPersistent();
        pet.setFollowing(true);
        pet.setHealth((float) pet.getMaxHealth());
        // Tang nhe de thay ro luc spawn (het sau 10 giay).
        pet.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false));
        world.spawnEntity(pet);
        player.sendMessage(Text.literal("§b[" + name + "]§r Da xuat hien! Toi se di theo va chien dau cung ban. "
                + "Dung /gf help de xem lenh."), false);
        return 1;
    }

    public static int setFollow(ServerPlayerEntity player, boolean follow) {
        GfCompanionEntity pet = findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        pet.setFollowing(follow);
        String name = GfConfig.get().companionName;
        player.sendMessage(Text.literal(follow
                ? "§b[" + name + "]§r Ro! Toi se di theo ban."
                : "§b[" + name + "]§r Ro! Toi se ngoi yen o day."), false);
        return 1;
    }

    /** Dich chuyen companion den toa do chi dinh va cho stay tai do. */
    public static int goTo(ServerPlayerEntity player, int x, int y, int z) {
        GfCompanionEntity pet = findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        pet.setFollowing(false);
        pet.requestTeleport(x + 0.5, y, z + 0.5);
        String name = GfConfig.get().companionName;
        player.sendMessage(Text.literal("§b[" + name + "]§r Da den (" + x + ", " + y + ", " + z
                + ") va dung yen. Dung /gf follow de goi di theo tiep."), false);
        return 1;
    }

    /** Keo companion ve canh player. */
    public static int bringHere(ServerPlayerEntity player) {
        GfCompanionEntity pet = findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        pet.requestTeleport(player.getX() + 1.0, player.getY(), player.getZ() + 1.0);
        pet.setFollowing(true);
        String name = GfConfig.get().companionName;
        player.sendMessage(Text.literal("§b[" + name + "]§r Co toi day!"), false);
        return 1;
    }

    /** Cho companion bien mat (khong rot do). */
    public static int dismiss(ServerPlayerEntity player) {
        GfCompanionEntity pet = findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Khong thay companion gan day."), false);
            return 0;
        }
        String name = GfConfig.get().companionName;
        pet.discard();
        player.sendMessage(Text.literal("§b[" + name + "]§r Tam biet! Can thi goi /gf spawn nhe."), false);
        return 1;
    }

    /**
     * Tick moi 20 tick: neu dang follow ma cach chu qua xa
     * (vuot teleportDistance) thi dich chuyen ve canh chu,
     * tranh ket lai ben kia nui/song.
     */
    public static void tickFarTeleport(ServerPlayerEntity player) {
        GfCompanionEntity pet = findOwned(player);
        if (pet == null || !pet.isFollowing() || pet.isSitting()) {
            return;
        }
        double max = GfConfig.get().teleportDistance;
        if (pet.squaredDistanceTo(player) > max * max) {
            pet.requestTeleport(player.getX() + 1.0, player.getY(), player.getZ() + 1.0);
        }
    }
}
