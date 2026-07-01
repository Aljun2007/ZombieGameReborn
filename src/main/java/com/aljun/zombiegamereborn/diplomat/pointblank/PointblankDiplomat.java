package com.aljun.zombiegamereborn.diplomat.pointblank;

import com.aljun.zombiegamereborn.diplomat.Diplomat;

public class PointblankDiplomat extends Diplomat {

    private IPointblankProvider provider;

    @Override
    public String getModID() {
        return "pointblank";
    }

    @Override
    public void init() {
        super.init();
        if (this.isLoaded()) {
            try {
                Class<?> implClass = Class.forName(
                        "com.aljun.zombiegamereborn.diplomat.pointblank.PointblankProviderImpl"
                );
                this.provider = (IPointblankProvider) implClass.getDeclaredConstructor().newInstance();
                this.provider.registerCallback();
            } catch (Exception e) {
                // 反射加载失败，provider 保持 null
            }
        }
    }
}
