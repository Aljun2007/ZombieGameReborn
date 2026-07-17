package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.register.ZGRRegistries;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

public class SummonZombieCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("summonZombie")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("zombie", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggestResource(ZGRRegistries.ZOMBIE_TYPE.get().getKeys(), builder)
                                )
                                .executes(context -> execute(context, 1, null))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> execute(context,
                                                IntegerArgumentType.getInteger(context, "count"), null))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(context -> execute(context,
                                                        IntegerArgumentType.getInteger(context, "count"),
                                                        BlockPosArgument.getLoadedBlockPos(context, "pos")))
                                        )
                                )
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> execute(context, 1,
                                                BlockPosArgument.getLoadedBlockPos(context, "pos")))
                                )
                        )
                );

        root.then(command);
    }

    private static int execute(CommandContext<CommandSourceStack> context, int defaultCount, BlockPos defaultPos) throws CommandSyntaxException {
        EntityType<?> entityType = ResourceArgument.getSummonableEntityType(context, "zombie").get();
        Entity test = entityType.create(context.getSource().getLevel());
        if (!(test instanceof Zombie)) {
            context.getSource().sendFailure(
                    Component.translatable("command.zombiegamereborn.summon.invalid_entity", EntityType.getKey(entityType).toString())
            );
            return 0;
        }
        ResourceLocation typeId = ResourceLocationArgument.getId(context, "type");
        if (ZombieType.getById(typeId) == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.zombiegamereborn.summon.unknown_type", typeId)
            );
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();

        BlockPos spawnBase = defaultPos != null ? defaultPos : BlockPos.containing(context.getSource().getPosition()).above();
        int count = Math.max(1, defaultCount);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = entityType.create(level);
            if (!(entity instanceof Zombie zombie)) return 0;
            ZombieTypeManager.initializeZombie(zombie, typeId);

            double offsetX = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;
            zombie.setPos(
                    spawnBase.getX() + 0.5 + offsetX,
                    spawnBase.getY(),
                    spawnBase.getZ() + 0.5 + offsetZ
            );
            level.addFreshEntity(zombie);
            spawned++;
        }

        if (spawned == 0) {
            context.getSource().sendFailure(Component.translatable("command.zombiegamereborn.summon.failed_to_create"));
            return 0;
        }

        if (spawned == 1) {
            context.getSource().sendSuccess(() ->
                            Component.translatable("command.zombiegamereborn.summon.success_with_pos", spawnBase.toShortString(), typeId.toString()),
                    true
            );
        } else {
            int finalSpawned = spawned;
            context.getSource().sendSuccess(() ->
                            Component.translatable("command.zombiegamereborn.summon.success_with_count", finalSpawned, typeId.getPath()),
                    true
            );
        }

        return spawned;
    }
}
