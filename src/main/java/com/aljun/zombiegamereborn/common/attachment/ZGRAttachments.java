package com.aljun.zombiegamereborn.common.attachment;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieData;
import com.aljun.zombiegamereborn.common.player.capability.PlayerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ZGRAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ZombieGameReborn.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZombieData>> ZOMBIE_DATA =
            ATTACHMENT_TYPES.register(
                    "zombie_data",
                    () -> AttachmentType.serializable(ZombieData::new).build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register(
                    "player_data",
                    () -> AttachmentType.serializable(PlayerData::new).build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
