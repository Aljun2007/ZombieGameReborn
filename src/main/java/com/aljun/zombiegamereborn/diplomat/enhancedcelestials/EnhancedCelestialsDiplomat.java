package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class EnhancedCelestialsDiplomat extends Diplomat {

    private IEnhancedCelestialsProvider provider = null;

    // 缓存
    private long cacheDay = -1;
    private boolean cachedIsBloodMoon = false;

    @Override
    public String getModID() {
        return "enhancedcelestials";
    }

    @Override
    public void init() {
        super.init();
        if (this.isLoaded()) {
            try {
                Class<?> implClass = Class.forName(
                        "com.aljun.zombiegamereborn.diplomat.enhancedcelestials.EnhancedCelestialsProviderImpl"
                );
                this.provider = (IEnhancedCelestialsProvider) implClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // 反射加载失败，provider 保持 null，所有方法自动跳过
            }
        }
    }

    public void setBloodMoon(MinecraftServer server) {
        if (provider != null) {
            provider.setBloodMoon(server);
            invalidateCache();
        }
    }

    public boolean isBloodMoon(MinecraftServer server) {
        if (provider == null) return false;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return false;
        long currentDay = overworld.getDayTime() / 24000;
        if (currentDay != cacheDay) {
            cacheDay = currentDay;
            cachedIsBloodMoon = provider.isBloodMoon(server);
        }
        return cachedIsBloodMoon;
    }

    public void invalidateCache() {
        cacheDay = -1;
        cachedIsBloodMoon = false;
    }
}
