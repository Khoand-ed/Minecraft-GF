package com.khoand.mcgf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Module 6 — Viec tu dong theo job: minevein (dao via quang), chop (don cay),
 * farm (thu lua + trong lai). Do roi ra dat dung survival, pet hut vao kho
 * rieng; chu lay lai bang /gf give hoac cat ruong bang /gf deposit.
 *
 * Moi job toi da 32 block, 1 block / 4 tick, tam quet 24 block.
 */
public final class GfAutoSkills {
    private static final int MAX_BLOCKS = 32;
    private static final int SCAN_RADIUS = 24;
    private static final int TICKS_PER_BLOCK = 4;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private GfAutoSkills() {
    }

    private enum Kind {
        MINEVEIN, CHOP, FARM
    }

    private static final class Job {
        final Kind kind;
        final Deque<BlockPos> queue = new ArrayDeque<>();
        int wait;
        int done;
        String targetName = "?";

        Job(Kind kind) {
            this.kind = kind;
        }
    }

    // ---------- Tao job ----------

    public static int startMinevein(ServerPlayerEntity player) {
        return start(player, Kind.MINEVEIN);
    }

    public static int startChop(ServerPlayerEntity player) {
        return start(player, Kind.CHOP);
    }

    public static int startFarm(ServerPlayerEntity player) {
        return start(player, Kind.FARM);
    }

    private static int start(ServerPlayerEntity player, Kind kind) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        if (JOBS.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Tui đang làm dở việc khác, gõ /gf stop để hủy rồi gọi lại!"), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        Job job = new Job(kind);
        BlockPos center = pet.getBlockPos();
        if (kind == Kind.FARM) {
            collectMatureCrops(world, center, 8, job);
        } else if (kind == Kind.CHOP) {
            BlockPos log = nearestMatching(world, center, SCAN_RADIUS,
                    s -> s.isIn(BlockTags.LOGS));
            if (log != null) {
                job.targetName = world.getBlockState(log).getBlock().getName().getString();
                floodSame(world, log, job, s -> s.isIn(BlockTags.LOGS));
            }
        } else {
            BlockPos ore = nearestMatching(world, center, SCAN_RADIUS, GfAutoSkills::isOre);
            if (ore != null) {
                job.targetName = world.getBlockState(ore).getBlock().getName().getString();
                Block oreBlock = world.getBlockState(ore).getBlock();
                floodSame(world, ore, job, s -> s.isOf(oreBlock));
            }
        }
        if (job.queue.isEmpty()) {
            String what = kind == Kind.FARM ? "ruộng chín" : kind == Kind.CHOP ? "cây" : "vỉa quặng";
            int r = kind == Kind.FARM ? 8 : SCAN_RADIUS;
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Quanh đây (" + r + " block) không thấy " + what + " nào!"), false);
            return 0;
        }
        pet.setSitting(false);
        JOBS.put(player.getUuid(), job);
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Rõ! Bắt đầu "
                + jobLabel(kind) + " (" + job.queue.size() + " block), đồ tui giữ trong kho, xong gõ /gf give nha!"),
                false);
        return 1;
    }

    private static String jobLabel(Kind kind) {
        if (kind == Kind.FARM) {
            return "thu hoạch";
        }
        if (kind == Kind.CHOP) {
            return "đốn cây";
        }
        return "đào quặng";
    }

    /** Huy job dang chay (dung chung voi /gf stop danh quai). */
    public static boolean cancel(ServerPlayerEntity player) {
        return JOBS.remove(player.getUuid()) != null;
    }

    public static boolean hasJob(ServerPlayerEntity player) {
        return JOBS.containsKey(player.getUuid());
    }

    // ---------- Tick ----------

    /** Goi moi server tick tu MinecraftGFMod. */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        for (var it = JOBS.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            GfCompanionEntity pet = GfCompanionManager.findOwned(player);
            if (pet == null || pet.isRemoved()) {
                it.remove();
                player.sendMessage(Text.literal("§c[MCGF]§r Mất companion nên hủy việc!"), false);
                continue;
            }
            Job job = e.getValue();
            if (++job.wait < TICKS_PER_BLOCK) {
                continue;
            }
            job.wait = 0;
            if (job.queue.isEmpty()) {
                it.remove();
                finish(player, pet, job);
                continue;
            }
            workOne(player, pet, job, job.queue.pollFirst());
            if (job.queue.isEmpty()) {
                it.remove();
                finish(player, pet, job);
            }
        }
    }

    private static void finish(ServerPlayerEntity player, GfCompanionEntity pet, Job job) {
        pet.swingHand(Hand.MAIN_HAND);
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Xong "
                + jobLabel(job.kind) + ": " + job.done + " block, kho tui có "
                + GfInv.countItems(pet.getBag()) + " món. Gõ /gf give để lấy!"), false);
    }

    // ---------- Lam 1 block ----------

    private static void workOne(ServerPlayerEntity player, GfCompanionEntity pet, Job job, BlockPos pos) {
        ServerWorld world = player.getServerWorld();
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        // Keo pet lai gan cho lam (giữ lao động trong tầm mắt).
        Vec3d c = Vec3d.ofCenter(pos);
        if (pet.squaredDistanceTo(c) > 36.0) {
            BlockPos stand = pos.up();
            if (!world.getBlockState(stand).isAir()) {
                stand = pos;
            }
            Vec3d s = Vec3d.ofCenter(stand);
            pet.requestTeleport(s.x, s.y, s.z);
        }
        pet.swingHand(Hand.MAIN_HAND);
        world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.1);

        if (job.kind == Kind.FARM && state.getBlock() instanceof CropBlock crop && crop.isMature(state)) {
            // Thu lua chin: roi hat dung survival roi trong lai tuoi 0.
            BlockEntity be = world.getBlockEntity(pos);
            for (ItemStack drop : Block.getDroppedStacks(state, world, pos, be)) {
                Block.dropStack(world, pos, drop);
            }
            world.setBlockState(pos, crop.withAge(0));
        } else {
            BlockEntity be = world.getBlockEntity(pos);
            for (ItemStack drop : Block.getDroppedStacks(state, world, pos, be)) {
                Block.dropStack(world, pos, drop);
            }
            world.removeBlock(pos, false);
        }
        job.done++;
        magnetToBag(world, pet);
    }

    /** Hut do roi trong 4 block vao kho pet. */
    private static void magnetToBag(ServerWorld world, GfCompanionEntity pet) {
        Box box = pet.getBoundingBox().expand(4.0);
        for (ItemEntity ie : world.getEntitiesByClass(ItemEntity.class, box,
                e -> !e.isRemoved() && !e.getStack().isEmpty())) {
            ItemStack copy = ie.getStack().copy();
            int before = copy.getCount();
            int left = GfInv.insert(pet.getBag(), copy);
            if (copy.getCount() < before) {
                if (left <= 0) {
                    ie.discard();
                } else {
                    ie.setStack(copy);
                }
            }
        }
    }

    // ---------- Quet the gioi ----------

    private interface Matcher {
        boolean test(BlockState state);
    }

    private static boolean isOre(BlockState s) {
        return s.isIn(BlockTags.COAL_ORES) || s.isIn(BlockTags.IRON_ORES)
                || s.isIn(BlockTags.COPPER_ORES) || s.isIn(BlockTags.GOLD_ORES)
                || s.isIn(BlockTags.REDSTONE_ORES) || s.isIn(BlockTags.LAPIS_ORES)
                || s.isIn(BlockTags.DIAMOND_ORES) || s.isIn(BlockTags.EMERALD_ORES)
                || s.isIn(BlockTags.NETHER_GOLD_ORES);
    }

    private static BlockPos nearestMatching(ServerWorld world, BlockPos center, int r, Matcher m) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            if (!m.test(world.getBlockState(p))) {
                continue;
            }
            double d = p.getSquaredDistance(center);
            if (d < bestDist) {
                bestDist = d;
                best = p.toImmutable();
            }
        }
        return best;
    }

    private static void floodSame(ServerWorld world, BlockPos start, Job job, Matcher m) {
        Deque<BlockPos> open = new ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        open.add(start);
        seen.add(start);
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!open.isEmpty() && job.queue.size() < MAX_BLOCKS) {
            BlockPos cur = open.pollFirst();
            BlockState s = world.getBlockState(cur);
            if (!m.test(s)) {
                continue;
            }
            if (cur.getSquaredDistance(start) > SCAN_RADIUS * SCAN_RADIUS) {
                continue;
            }
            job.queue.addLast(cur.toImmutable());
            for (int[] d : dirs) {
                BlockPos n = cur.add(d[0], d[1], d[2]);
                if (seen.add(n)) {
                    open.addLast(n);
                }
            }
        }
    }

    private static void collectMatureCrops(ServerWorld world, BlockPos center, int r, Job job) {
        for (BlockPos p : BlockPos.iterate(center.add(-r, -3, -r), center.add(r, 3, r))) {
            if (job.queue.size() >= MAX_BLOCKS) {
                break;
            }
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && crop.isMature(s)) {
                job.queue.addLast(p.toImmutable());
            }
        }
        if (!job.queue.isEmpty()) {
            job.targetName = "ruộng";
        }
    }

    // ---------- give / deposit / bag ----------

    /** Pet dua het do trong kho cho chu; thua thi roi duoi chan chu. */
    public static int give(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        Inventory bag = pet.getBag();
        int moved = 0;
        for (int i = 0; i < bag.size(); i++) {
            ItemStack s = bag.getStack(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack copy = s.copy();
            player.getInventory().insertStack(copy);
            if (copy.isEmpty()) {
                bag.setStack(i, ItemStack.EMPTY);
            } else {
                int before = s.getCount();
                bag.setStack(i, copy);
                if (copy.getCount() < before) {
                    moved++;
                }
                continue;
            }
            moved++;
        }
        bag.markDirty();
        // Phan con thua (tui chu day): roi duoi chan.
        ServerWorld world = player.getServerWorld();
        int dropped = 0;
        for (int i = 0; i < bag.size(); i++) {
            ItemStack s = bag.getStack(i);
            if (!s.isEmpty()) {
                Block.dropStack(world, player.getBlockPos(), s.copy());
                bag.setStack(i, ItemStack.EMPTY);
                dropped++;
            }
        }
        bag.markDirty();
        if (moved == 0 && dropped == 0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Kho tui trống trơn!"), false);
            return 0;
        }
        String msg = "§b[" + GfConfig.get().companionName + "]§r Đưa bạn " + moved + " đống"
                + (dropped > 0 ? " (túi bạn đầy nên " + dropped + " đống rơi dưới chân)" : "") + "!";
        player.sendMessage(Text.literal(msg), false);
        return 1;
    }

    /**
     * Cat do tu kho pet vao ruong/thung gan nhat (8 block quanh pet).
     * Ruong doi chi tinh nua ke ben (van du de dung).
     */
    public static int deposit(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        if (GfInv.countItems(pet.getBag()) == 0) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Kho tui trống, khỏi cất!"), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        BlockPos center = pet.getBlockPos();
        Inventory chest = null;
        BlockPos chestPos = null;
        outer:
        for (BlockPos p : BlockPos.iterate(center.add(-8, -4, -8), center.add(8, 4, 8))) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof Inventory inv) {
                chest = inv;
                chestPos = p.toImmutable();
                break outer;
            }
        }
        if (chest == null) {
            player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName
                    + "]§r Quanh tui (8 block) không có rương/thùng nào!"), false);
            return 0;
        }
        Inventory bag = pet.getBag();
        int moved = 0;
        for (int i = 0; i < bag.size(); i++) {
            ItemStack s = bag.getStack(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack copy = s.copy();
            int left = GfInv.insert(chest, copy);
            if (left < s.getCount()) {
                moved++;
                if (left <= 0) {
                    bag.setStack(i, ItemStack.EMPTY);
                } else {
                    copy.setCount(left);
                    bag.setStack(i, copy);
                }
            }
        }
        bag.markDirty();
        int left = GfInv.countItems(bag);
        String where = chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ();
        player.sendMessage(Text.literal("§b[" + GfConfig.get().companionName + "]§r Cất " + moved
                + " đống vào kho tại (" + where + ")" + (left > 0 ? ", còn " + left + " món trong túi tui" : "") + "!"),
                false);
        return 1;
    }

    /** Liet ke kho pet tren chat. */
    public static int showBag(ServerPlayerEntity player) {
        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
        if (pet == null) {
            player.sendMessage(Text.literal("§c[MCGF]§r Chua co companion. Dung /gf spawn truoc."), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("§b[MCGF] Kho pet:§r");
        boolean empty = true;
        for (int i = 0; i < pet.getBag().size(); i++) {
            ItemStack s = pet.getBag().getStack(i);
            if (!s.isEmpty()) {
                empty = false;
                sb.append("\n§7- §e").append(s.getCount()).append("x ").append(s.getName().getString());
            }
        }
        if (empty) {
            sb.append(" trống trơn!");
        } else {
            sb.append("\n§7Gõ /gf give để lấy, /gf deposit để cất rương.");
        }
        player.sendMessage(Text.literal(sb.toString()), false);
        return 1;
    }
}
