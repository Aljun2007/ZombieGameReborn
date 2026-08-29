package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedCelestialsDiplomat extends Diplomat {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCelestialsDiplomat.class);

    // 新版 Enhanced Celestials 2: Core
    private static final String MOD_ID_EC2_CORE = "enhancedcelestials2core";
    // 旧版 Enhanced Celestials
    private static final String MOD_ID_EC_LEGACY = "enhancedcelestials";

    private IEnhancedCelestialsProvider provider = null;
    private String activeModId = null;

    // 缓存
    private long cacheDay = -1;
    private boolean cachedIsBloodMoon = false;

    @Override
    public String getModID() {
        return MOD_ID_EC2_CORE;
    }

    @Override
    public void init() {
        // 优先新版 EC2，其次回退旧版 EC，均未安装则跳过联动
        if (ModList.get().isLoaded(MOD_ID_EC2_CORE)) {
            if (tryLoadProvider("EnhancedCelestials2ProviderImpl", MOD_ID_EC2_CORE)) {
                return;
            }
        }
        if (ModList.get().isLoaded(MOD_ID_EC_LEGACY)) {
            tryLoadProvider("EnhancedCelestialsProviderImpl", MOD_ID_EC_LEGACY);
        }
        if (!this.isLoaded()) {
            LOGGER.info("[ZGR] 模组 '{}' 与 '{}' 均未安装，跳过血月联动", MOD_ID_EC2_CORE, MOD_ID_EC_LEGACY);
        }
    }

    private boolean tryLoadProvider(String className, String modId) {
        try {
            Class<?> implClass = Class.forName(
                    "com.aljun.zombiegamereborn.diplomat.enhancedcelestials." + className
            );
            this.provider = (IEnhancedCelestialsProvider) implClass.getDeclaredConstructor().newInstance();
            this.activeModId = modId;
            LOGGER.info("[ZGR] 检测到模组 '{}' 已安装，启用血月联动", modId);
            return true;
        } catch (Exception e) {
            // 反射加载失败，provider 保持 null，本次尝试失败
            LOGGER.warn("[ZGR] 模组 '{}' 联动加载失败: {}", modId, e.toString());
            this.provider = null;
            this.activeModId = null;
            return false;
        }
    }

    @Override
    public boolean isLoaded() {
        return provider != null;
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