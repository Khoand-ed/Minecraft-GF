package com.khoand.mcgf;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

/**
 * Module 1 — Lenh /gf.
 * Muc tieu: kiem tra mod da load tren server 1.21.1, lam nen cho Module 2-4.
 *
 * <pre>
 * /gf help              — xem tro giup
 * /gf hello             — test companion tra loi
 * /gf version           — xem version
 * /gf name &lt;ten&gt;        — doi ten companion (luu config)
 * /gf say &lt;noi dung&gt;    — companion nhac lai (Module 3 se thay bang LLM)
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
                                            + "§7M2=follow, M3=AI chat, M4=sinh ton§r"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("hello").executes(ctx -> {
                            String name = GfConfig.get().companionName;
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[" + name + "]§r Xin chao! Toi dang chay Module 1 (core 1.21.1). "
                                            + "M2 toi se biet di theo ban!"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("version").executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§b[MCGF]§r 0.1.0-m1 | MC 1.21.1 Fabric | Java 21"),
                                    false);
                            return 1;
                        }))
                        .then(CommandManager.literal("name")
                                .then(CommandManager.argument("ten", StringArgumentType.word()).executes(ctx -> {
                                    String ten = StringArgumentType.getString(ctx, "ten");
                                    GfConfig.get().companionName = ten;
                                    GfConfig.save();
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
                                            // Tam echo — Module 3 se goi Gemini/OpenAI o day.
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("§b[" + name + "]§r " + text),
                                                    false);
                                            return 1;
                                        })))
        ));
    }
}
