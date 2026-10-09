package com.aljun.zombiegamereborn.register;

import com.aljun.zombiegamereborn.common.commands.ZGRCommands;
import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.ZGRConfigFileManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;


@EventBusSubscriber
public class ZGRSpecialRegisterEvents {
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        GameProperty gameProperty = ZGRConfigFileManager.loadConfig(event.getServer());
        ZGRGame.newGameProperty(gameProperty);
        ZombieStatic.resetZombieCount();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralCommandNode<CommandSourceStack> cmd = ZGRCommands.registry(dispatcher, event.getBuildContext());
    }
}
