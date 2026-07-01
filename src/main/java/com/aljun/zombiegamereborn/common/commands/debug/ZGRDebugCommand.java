package com.aljun.zombiegamereborn.common.commands.debug;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.debug.ZGRDebug;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ZGRDebugCommand implements Command<CommandSourceStack> {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal("debug").requires(context -> ZGRDebug.isDebugMode());
        load(debugCommand);
        root.then(debugCommand);
    }

    private static void load(LiteralArgumentBuilder<CommandSourceStack> command) {
        command.then(Commands.literal("debug_items").executes((context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            debugItems(player);
            return 0;
        })));
    }

    //为游戏中添加调试快捷工具，一共包含 调时间（day, night）、秒杀鱼、强物品、调试棒、生命血包
    private static void debugItems(ServerPlayer player) {
        ItemStack killer = new ItemStack(Items.BLAZE_ROD);
        killer.setHoverName(Component.nullToEmpty("KillerItem"));
        killer.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("killer"));
        player.addItem(killer);

        ItemStack snatcher = new ItemStack(Items.TROPICAL_FISH);
        snatcher.setHoverName(Component.nullToEmpty("SnatcherItem"));
        snatcher.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("snatcher"));
        player.addItem(snatcher);

        ItemStack heal = new ItemStack(Items.RED_DYE);
        heal.setHoverName(Component.nullToEmpty("HealItem"));
        heal.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("heal"));
        player.addItem(heal);

        ItemStack day = new ItemStack(Items.GOLD_NUGGET);
        day.setHoverName(Component.nullToEmpty("DayItem"));
        day.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("day"));
        player.addItem(day);

        ItemStack night = new ItemStack(Items.COAL);
        night.setHoverName(Component.nullToEmpty("NightItem"));
        night.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("night"));
        player.addItem(night);

        ItemStack test = new ItemStack(Items.STICK);
        test.setHoverName(Component.nullToEmpty("TestItem"));
        test.getOrCreateTag().put(ZombieGameReborn.MOD_ID + ".debug.itemtype", StringTag.valueOf("test"));
        player.addItem(test);

    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        return 0;
    }
}
