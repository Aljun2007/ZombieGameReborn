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
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
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
