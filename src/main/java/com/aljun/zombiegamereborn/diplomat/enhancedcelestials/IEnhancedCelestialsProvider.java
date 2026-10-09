package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import net.minecraft.server.MinecraftServer;

public interface IEnhancedCelestialsProvider {
    void setBloodMoon(MinecraftServer server);
    boolean isBloodMoon(MinecraftServer server);
}
