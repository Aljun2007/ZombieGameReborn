package com.aljun.zombiegamereborn.diplomat;

import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Diplomat {
    private static final Logger LOGGER = LoggerFactory.getLogger(Diplomat.class);

    public abstract String getModID();

    private boolean isLoaded = false;

    public boolean isLoaded() {
        return isLoaded;
    }

    public void init() {
        this.isLoaded = ModList.get().isLoaded(getModID());
        if (this.isLoaded) {
            LOGGER.info("[ZGR] 检测到模组 '{}' 已安装，启用联动功能", getModID());
        } else {
            LOGGER.info("[ZGR] 模组 '{}' 未安装，跳过联动", getModID());
        }
    }
}
