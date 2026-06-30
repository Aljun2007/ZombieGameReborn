package com.aljun.zombiegamereborn.diplomat.tacz;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
import net.minecraftforge.common.MinecraftForge;

public class TACZDiplomat extends Diplomat {

    private ITaczProvider provider = null;

    @Override
    public String getModID() {
        return "tacz";
    }

    @Override
    public void init() {
        super.init();
        if (this.isLoaded()) {
            try {
                Class<?> implClass = Class.forName(
                        "com.aljun.zombiegamereborn.diplomat.tacz.TaczProviderImpl"
                );
                this.provider = (ITaczProvider) implClass.getDeclaredConstructor().newInstance();
                this.provider.registerEvents(MinecraftForge.EVENT_BUS);
            } catch (Exception e) {
                // 反射加载失败，provider 保持 null
            }
        }
    }
}
