package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.player.PlayerStatic;
import com.aljun.zombiegamereborn.common.player.ReginalStageDetector;
import com.aljun.zombiegamereborn.common.player.TimeBroadcast;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class PlayerCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("player")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("day")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                            if (targets.isEmpty()) {
                                                context.getSource().sendFailure(
                                                        Component.translatable("command.zombiegamereborn.player.setday.no_targets")
                                                );
                                                return 0;
                                            }

                                            for (ServerPlayer player : targets) {
                                                IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);
                                                long day = playerData != null ? playerData.getSurvivedDay() : 1L;
                                                context.getSource().sendSuccess(
                                                        () -> Component.translatable("command.zombiegamereborn.player.day.get", player.getName().getString(), day),
                                                        false
                                                );
                                            }
                                            return targets.size();
                                        })
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("day", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                                .executes(context -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                                    int day = IntegerArgumentType.getInteger(context, "day");

                                                    if (targets.isEmpty()) {
                                                        context.getSource().sendFailure(
                                                                Component.translatable("command.zombiegamereborn.player.setday.no_targets")
                                                        );
                                                        return 0;
                                                    }

                                                    for (ServerPlayer player : targets) {
                                                        IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);
                                                        if (playerData != null) {
                                                            playerData.setSurvivedDay(day);
                                                            ZGRPlayerAPI.saveToPersistentData(player);
                                                        }
                                                    }

                                                    context.getSource().sendSuccess(
                                                            () -> Component.translatable("command.zombiegamereborn.player.setday.success", targets.size(), day),
                                                            true
                                                    );
                                                    return targets.size();
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("getGlobalAverageDouble")
                                .executes(context -> {
                                    double avg = ReginalStageDetector.getGlobalAverage(context.getSource().getServer());
                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("command.zombiegamereborn.player.day.global_average", String.format("%.1f", avg)),
                                            false
                                    );
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("isOnSurface")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    if (targets.isEmpty()) {
                                        context.getSource().sendFailure(
                                                Component.translatable("command.zombiegamereborn.player.setday.no_targets")
                                        );
                                        return 0;
                                    }

                                    for (ServerPlayer player : targets) {
                                        boolean onSurface = PlayerStatic.isOnSurfaceOfOverworld(player);
                                        String status = onSurface ? "command.zombiegamereborn.player.isonsurface.true"
                                                : "command.zombiegamereborn.player.isonsurface.false";
                                        context.getSource().sendSuccess(
                                                () -> Component.translatable("command.zombiegamereborn.player.isonsurface",
                                                        player.getName().getString(),
                                                        Component.translatable(status)),
                                                false
                                        );
                                    }
                                    return targets.size();
                                })
                        )
                )
                .then(Commands.literal("playTimeBroadCast")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    if (targets.isEmpty()) {
                                        context.getSource().sendFailure(
                                                Component.translatable("command.zombiegamereborn.player.setday.no_targets")
                                        );
                                        return 0;
                                    }

                                    for (ServerPlayer player : targets) {
                                        TimeBroadcast.handleManualClockUse(player);
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("command.zombiegamereborn.player.playtimebroadcast", targets.size()),
                                            true
                                    );
                                    return targets.size();
                                })
                        )
                )
        );
    }
}
