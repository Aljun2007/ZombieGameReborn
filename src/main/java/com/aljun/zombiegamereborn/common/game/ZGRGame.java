package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.utils.GamePropertyPresentUtils;
import net.minecraft.server.MinecraftServer;

public class ZGRGame {
    private static GameProperty gameProperty = GamePropertyPresentUtils.disabled();

    public static void newGameProperty(GameProperty gameProperty) {
        ZGRGame.gameProperty = gameProperty;
        gameProperty.init();
    }

    public static GameProperty getGameProperty() {
        return gameProperty;
    }

    public static class Rules {
        public static boolean canZombieBreakBlock(MinecraftServer server) {
            return gameProperty.canZombieBreakBlock;
        }

        public static boolean canZombiePlaceBlock(MinecraftServer server) {
            return gameProperty.canZombiePlaceBlock;
        }
    }
}
