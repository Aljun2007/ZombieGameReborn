package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.register.ZGRRegistries;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

public class NewZombieCommand {
    
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("newZombie")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", ResourceLocationArgument.id())
                        .suggests((context, builder) -> {
                            return SharedSuggestionProvider.suggestResource(ZGRRegistries.ZOMBIE_TYPE.get().getKeys(), builder);
                        }
                        )
                        .executes(NewZombieCommand::execute)
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(NewZombieCommand::executeWithPosition)
                        )
                        .then(Commands.literal("count")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(NewZombieCommand::executeWithCount)
                                )
                        )
                );

        root.then(command);
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeWithPosition(context);
    }
    
    private static int executeWithPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation typeId = ResourceLocationArgument.getId(context, "type");
        
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos;
        
        try {
            pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        } catch (IllegalArgumentException e) {
            pos = BlockPos.containing(context.getSource().getPosition());
        }
        
        BlockPos spawnPos = pos.above();
        
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            context.getSource().sendFailure(Component.literal("无法创建僵尸实体"));
            return 0;
        }

        ZombieTypeManager.initializeZombie(zombie,typeId);

        zombie.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        level.addFreshEntity(zombie);


        BlockPos finalSpawnPos = spawnPos;
        context.getSource().sendSuccess(() ->
            Component.literal("成功生成僵尸类型: " + typeId + " 在位置 " + finalSpawnPos.toShortString()),
            true
        );
        
        return 1;
    }
    
    private static int executeWithCount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation typeId = ResourceLocationArgument.getId(context, "type");
        int count = IntegerArgumentType.getInteger(context, "count");
        
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPos.containing(context.getSource().getPosition()).above();
        
        String typeString = typeId.getPath();
        
        for (int i = 0; i < count; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetZ = (Math.random() - 0.5) * 2;
                zombie.setPos(pos.getX() + 0.5 + offsetX, pos.getY(), pos.getZ() + 0.5 + offsetZ);
                ZombieTypeManager.initializeZombie(zombie,typeId);
                level.addFreshEntity(zombie);
            }
        }
        
        context.getSource().sendSuccess(() ->
            Component.literal("成功生成 " + count + " 个僵尸类型: " + typeString),
            true
        );
        
        return 1;
    }
}