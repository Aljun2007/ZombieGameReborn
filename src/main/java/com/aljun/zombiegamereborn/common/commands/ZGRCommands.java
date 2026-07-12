package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.commands.debug.ZGRDebugCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ZGRCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> ROOT = Commands.literal(ZombieGameReborn.MOD_ID);

    public static LiteralCommandNode<CommandSourceStack> registry(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        ConfigCommand.register(ROOT);
        ZGRDebugCommand.register(ROOT);
        SummonZombieCommand.register(ROOT, buildContext);
        return dispatcher.register(ROOT);
    }
}
