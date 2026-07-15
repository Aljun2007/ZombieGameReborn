package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.events.handler.SurvivedDayHandler;
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
                .then(Commands.literal("setDay")
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

                                            SurvivedDayHandler.setSurvivedDays(day);

                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("command.zombiegamereborn.player.setday.success", targets.size(), day),
                                                    true
                                            );
                                            return targets.size();
                                        })
                                )
                        )
                )
        );
    }
}
