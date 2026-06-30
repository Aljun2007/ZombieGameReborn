package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
import net.minecraft.world.level.Level;

public class EnhancedCelestialsDiplomat extends Diplomat {

    private IEnhancedCelestialsProvider provider = null;

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
                // 反射加载失败，provider 保持 null，setBloodMoon 自动跳过
            }
        }
    }

    public void setBloodMoon(Level level) {
        if (provider != null) {
            provider.setBloodMoon(level);
        }
    }
}
