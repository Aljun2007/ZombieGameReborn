package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.events.handler.GamePropertyRefresher;
import com.aljun.zombiegamereborn.debug.ZGRDebug;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ZGRDebugCommand implements Command<CommandSourceStack> {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal("debug") .requires(source -> source.hasPermission(2)&&ZGRDebug.isDebugMode());
        load(debugCommand);
        root.then(debugCommand);
    }

    private static void load(LiteralArgumentBuilder<CommandSourceStack> command) {
        command.then(Commands.literal("debug_items").executes((context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            debugItems(player);
            return 0;
        })));
        command.then(Commands.literal("heal").executes((context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.removeAllEffects();
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, 8));
            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 8));
            return 0;
        })));
        command.then(Commands.literal("clean_all_zombies").executes((context -> {
            ServerLevel level = context.getSource().getLevel();
            List<Zombie> zombies = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Zombie zombie) {
                    zombies.add(zombie);
                }
            }
            zombies.forEach(z -> z.remove(Entity.RemovalReason.DISCARDED));
            context.getSource().sendSuccess(() -> Component.translatable("command.zombiegamereborn.debug.clean_all_zombies", zombies.size()), true);
            return zombies.size();
        })));
        command.then(Commands.literal("setBloodMoon").executes((context -> {
            MinecraftServer server = context.getSource().getServer();
            ServerLevel overworld = server.overworld();
            long currentDay = overworld.getDayTime() / 24000;

            if (!ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isLoaded()) {
                context.getSource().sendFailure(Component.translatable("command.zombiegamereborn.debug.setbloodmoon.not_installed"));
                return 0;
            }

            if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(server)) {
                context.getSource().sendFailure(Component.translatable("command.zombiegamereborn.debug.setbloodmoon.already_blood_moon", currentDay));
                return 0;
            }

            ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.setBloodMoon(server);

            if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(server)) {
                GamePropertyRefresher.bloodMoonActive = true;
                GamePropertyRefresher.bloodMoonTriggeredThisDay = true;
                context.getSource().sendSuccess(() -> Component.translatable("command.zombiegamereborn.debug.setbloodmoon.success", currentDay), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.translatable("command.zombiegamereborn.debug.setbloodmoon.failed", currentDay));
                return 0;
            }
        })));
    }

    //为游戏中添加调试快捷工具，一共包含 调时间（day, night）、秒杀鱼、强物品、调试棒、生命血包
    private static void debugItems(ServerPlayer player) {
        ItemStack killer = new ItemStack(Items.BLAZE_ROD);
        killer.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("KillerItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, killer, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "killer"));
        player.addItem(killer);

        ItemStack snatcher = new ItemStack(Items.TROPICAL_FISH);
        snatcher.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("SnatcherItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, snatcher, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "snatcher"));
        player.addItem(snatcher);

        ItemStack heal = new ItemStack(Items.RED_DYE);
        heal.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("HealItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, heal, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "heal"));
        player.addItem(heal);

        ItemStack day = new ItemStack(Items.GOLD_NUGGET);
        day.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("DayItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, day, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "day"));
        player.addItem(day);

        ItemStack night = new ItemStack(Items.COAL);
        night.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("NightItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, night, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "night"));
        player.addItem(night);

        ItemStack test = new ItemStack(Items.STICK);
        test.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty("TestItem"));
        CustomData.update(DataComponents.CUSTOM_DATA, test, tag -> tag.putString(ZombieGameReborn.MOD_ID + ".debug.itemtype", "test"));
        player.addItem(test);

    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        return 0;
    }
}
