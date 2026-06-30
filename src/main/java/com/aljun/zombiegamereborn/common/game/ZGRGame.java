package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import net.minecraft.server.MinecraftServer;

public class ZGRGame {
    public static void newGameProperty(GameProperty gameProperty) {
        ZGRGame.gameProperty = gameProperty;
        gameProperty.init();
    }

    private static GameProperty gameProperty = GameProperty.empty();

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
