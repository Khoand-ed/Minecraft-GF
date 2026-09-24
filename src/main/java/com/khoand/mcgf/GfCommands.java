package com.khoand.mcgf;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Module 1 — Lenh /gf co ban. Module 2 — Lenh companion follow.
 * Module 3 — Lenh AI: ask, ai on/off, forget, apikey.
 * Module 4 — Lenh sinh ton: attack, stop, mine, collect, feed.
 *
 * <pre>
 * /gf help              — xem tro giup
 * /gf hello             — test companion tra loi
 * /gf version           — xem version
 * /gf name &lt;ten&gt;        — doi ten companion (luu config)
 * /gf say &lt;noi dung&gt;    — companion nhac lai
 * /gf spawn             — goi companion ra (M2)
 * /gf follow            — di theo (M2)
 * /gf stay              — ngoi yen (M2)
 * /gf here              — keo ve canh ban (M2)
 * /gf goto &lt;x&gt; &lt;y&gt; &lt;z&gt;  — den toa do va dung yen (M2)
 * /gf dismiss           — cho bien mat (M2)
 * /gf ask &lt;cau hoi&gt;     — hoi AI (M3, nhu chat @gf)
 * /gf ai on|off         — bat/tat AI online (M3, can OP)
 * /gf forget            — xoa tri nho hoi-dap (M3)
 * /gf apikey &lt;key&gt;     — nap key Gemini (M3, can OP, luu config)
 * Chat: @gf &lt;cau hoi&gt;    — hoi AI ngay trên chat (M3)
 * /gf attack            — danh quai gan nhat (M4)
 * /gf stop              — dung danh (M4)
 * /gf mine              — dao block dang nhin (M4)
 * /gf collect           — nhat do roi quanh ban (M4)
 * /gf feed              — cho an thit tu tui ban (M4)
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
                            String ai = GfBrain.isOnlineReady() ? "online (Gemini)" : "offline (chua co key)";
                            ctx.getSource().sendFeedback(() -> Text.literal(
                                    "§b[MCGF] Lenhs (AI: " + ai + "):\n"
                                            + "§e/gf hello§r — chao companion\n"
                                            + "§e/gf version§r — version mod\n"
                                            + "§e/gf name <ten>§r — doi ten\n"
                                            + "§e/gf say <text>§r — nhac lai\n"
                                            + "§e/gf spawn|follow|stay|here|dismiss§r — dieu companion\n"
                                            + "§e/gf goto <x> <y> <z>§r — den toa do\n"
                                            + "§e@gf <cau hoi>§r — chat voi AI (prefix hien tai)\n"
                                            + "§e/gf ask <cau hoi>§r — hoi AI\n"
                                            + "§e/gf forget§r — xoa tri nho\n"
                                            + "§e/gf ai on|off§r — bat/tat AI (OP)\n"
                                            + "§e/gf apikey <key>§r — nap key Gemini (OP)\n"
                                            + "§e/gf attack§r — danh quai gan nhat\n"
                                            + "§e/gf stop§r — dung danh\n"
                                            + "§e/gf mine§r — dao block dang nhin\n"
                                            + "§e/gf collect§r — nhat do roi\n"
                                            + "§e/gf feed§r — cho an"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("hello").executes(ctx -> {
                            String name = GfConfig.get().companionName;
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[" + name + "]§r Xin chao! Dung /gf spawn de goi toi ra, "
                                            + "chat " + GfConfig.get().chatPrefix + " <cau hoi> de tro chuyen!"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("version").executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[MCGF]§r 0.4.0-m4 | MC 1.21.1 Fabric | Java 21"),
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
                        // ---- Module 4: sinh ton ----
                        .then(CommandManager.literal("attack").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfSurvival.attack(player);
                        }))
                        .then(CommandManager.literal("stop").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfSurvival.stop(player);
                        }))
                        .then(CommandManager.literal("mine").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfSurvival.mine(player);
                        }))
                        .then(CommandManager.literal("collect").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfSurvival.collect(player);
                        }))
                        .then(CommandManager.literal("feed").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            return GfSurvival.feed(player);
                        }))
                        // ---- Module 3: AI ----
                        .then(CommandManager.literal("ask")
                                .then(CommandManager.argument("cauhoi", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if (player == null) {
                                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                                return 0;
                                            }
                                            GfBrain.ask(player, StringArgumentType.getString(ctx, "cauhoi"));
                                            return 1;
                                        })))
                        .then(CommandManager.literal("forget").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendError(Text.literal("Lenh nay chi dung in-game."));
                                return 0;
                            }
                            GfBrain.clearHistory(player);
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[MCGF]§r Da xoa tri nho hoi-dap."),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("ai")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("chedo", StringArgumentType.word()).executes(ctx -> {
                                    String chedo = StringArgumentType.getString(ctx, "chedo");
                                    if (chedo.equalsIgnoreCase("on")) {
                                        GfConfig.get().aiEnabled = true;
                                        GfConfig.save();
                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal("§b[MCGF]§r Da BAT AI online."),
                                                false);
                                        return 1;
                                    }
                                    if (chedo.equalsIgnoreCase("off")) {
                                        GfConfig.get().aiEnabled = false;
                                        GfConfig.save();
                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal("§b[MCGF]§r Da TAT AI online (chi tra loi offline)."),
                                                false);
                                        return 1;
                                    }
                                    ctx.getSource().sendError(Text.literal("Dung: /gf ai on|off"));
                                    return 0;
                                })))
                        .then(CommandManager.literal("apikey")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("key", StringArgumentType.word()).executes(ctx -> {
                                    String key = StringArgumentType.getString(ctx, "key");
                                    GfConfig.get().geminiApiKey = key;
                                    GfConfig.save();
                                    String masked = key.length() > 4 ? "****" + key.substring(key.length() - 4) : "****";
                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("§b[MCGF]§r Da luu key Gemini (" + masked
                                                    + "). Chat " + GfConfig.get().chatPrefix + " <cau hoi> de thu!"),
                                            false);
                                    return 1;
                                })))
        ));
    }
}
