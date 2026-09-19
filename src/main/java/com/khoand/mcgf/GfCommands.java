package com.khoand.mcgf;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Module 1 — Lenh /gf co ban. Module 2 — Lenh companion follow.
 *
 * <pre>
 * /gf help              — xem tro giup
 * /gf hello             — test companion tra loi
 * /gf version           — xem version
 * /gf name &lt;ten&gt;        — doi ten companion (luu config)
 * /gf say &lt;noi dung&gt;    — companion nhac lai (Module 3 se thay bang LLM)
 * /gf spawn             — goi companion ra (M2)
 * /gf follow            — di theo (M2)
 * /gf stay              — ngoi yen (M2)
 * /gf here              — keo ve canh ban (M2)
 * /gf goto &lt;x&gt; &lt;y&gt; &lt;z&gt;  — den toa do va dung yen (M2)
 * /gf dismiss           — cho bien mat (M2)
 * </pre>
 */
public final class GfCommands {
    private GfCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("gf")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[MCGF]§r Dung /gf help de xem lenh. Companion: "
                                            + GfConfig.get().companionName),
                                    false);
                            return 1;
                        })
                        .then(CommandManager.literal("help").executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal(
                                    "§b[MCGF] Lenhs:\n"
                                            + "§e/gf hello§r — chao companion\n"
                                            + "§e/gf version§r — version mod\n"
                                            + "§e/gf name <ten>§r — doi ten\n"
                                            + "§e/gf say <text>§r — nhac lai (tam, M3=AI)\n"
                                            + "§e/gf spawn§r — goi companion\n"
                                            + "§e/gf follow§r — di theo\n"
                                            + "§e/gf stay§r — ngoi yen\n"
                                            + "§e/gf here§r — keo ve canh ban\n"
                                            + "§e/gf goto <x> <y> <z>§r — den toa do\n"
                                            + "§e/gf dismiss§r — cho bien mat\n"
                                            + "§7M3=AI chat, M4=sinh ton§r"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("hello").executes(ctx -> {
                            String name = GfConfig.get().companionName;
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[" + name + "]§r Xin chao! Dung /gf spawn de goi toi ra, "
                                            + "/gf follow de toi di theo ban!"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("version").executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[MCGF]§r 0.2.0-m2 | MC 1.21.1 Fabric | Java 21"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("name")
                                .then(CommandManager.argument("ten", StringArgumentType.word()).executes(ctx -> {
                                    String ten = StringArgumentType.getString(ctx, "ten");
                                    GfConfig.get().companionName = ten;
                                    GfConfig.save();
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    if (player != null) {
                                        GfCompanionEntity pet = GfCompanionManager.findOwned(player);
                                        if (pet != null) {
                                            pet.setCustomName(Text.literal(ten));
                                        }
                                    }
                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("§b[MCGF]§r Da doi ten companion thanh: " + ten),
                                            false);
                                    return 1;
                                })))
                        .then(CommandManager.literal("say")
                                .then(CommandManager.argument("noidung", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String text = StringArgumentType.getString(ctx, "noidung");
                                            String name = GfConfig.get().companionName;
                                            // Tam echo — Module 3 se goi Gemini o day.
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("§b[" + name + "]§r " + text),
                                                    false);
                                            return 1;
                                        })))
                        // ---- Module 2: companion ----
                        .then(CommandManager.literal("spawn").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfCompanionManager.spawn(player);
                        }))
                        .then(CommandManager.literal("follow").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfCompanionManager.setFollow(player, true);
                        }))
                        .then(CommandManager.literal("stay").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfCompanionManager.setFollow(player, false);
                        }))
                        .then(CommandManager.literal("here").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfCompanionManager.bringHere(player);
                        }))
                        .then(CommandManager.literal("goto")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer(-30000000, 30000000))
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer(-64, 320))
                                                .then(CommandManager
                                                        .argument("z", IntegerArgumentType.integer(-30000000, 30000000))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                            if (player == null) {
                                                                ctx.getSource().sendError(
                                                                        Text.literal("Lenh nay chi dung in-game."));
                                                                return 0;
                                                            }
                                                            return GfCompanionManager.goTo(player,
                                                                    IntegerArgumentType.getInteger(ctx, "x"),
                                                                    IntegerArgumentType.getInteger(ctx, "y"),
                                                                    IntegerArgumentType.getInteger(ctx, "z"));
                                                        })))))
                        .then(CommandManager.literal("dismiss").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfCompanionManager.dismiss(player);
                        }))
        ));
    }
}
