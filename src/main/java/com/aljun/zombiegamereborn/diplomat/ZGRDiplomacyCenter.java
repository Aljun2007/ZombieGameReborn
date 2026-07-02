package com.aljun.zombiegamereborn.diplomat;

import com.aljun.zombiegamereborn.diplomat.enhancedcelestials.EnhancedCelestialsDiplomat;
import com.aljun.zombiegamereborn.diplomat.musketmod.MusketmodDiplomat;
import com.aljun.zombiegamereborn.diplomat.pointblank.PointblankDiplomat;
import com.aljun.zombiegamereborn.diplomat.tacz.TaczDiplomat;

public class ZGRDiplomacyCenter {
    public static final EnhancedCelestialsDiplomat ENHANCED_CELERESTIALS_DIPLOMAT = new EnhancedCelestialsDiplomat();
    public static final TaczDiplomat TACZ_DIPLOMAT = new TaczDiplomat();
    public static final MusketmodDiplomat MUSKETMOD_DIPLOMAT = new MusketmodDiplomat();
    public static final PointblankDiplomat POINTBLANK_DIPLOMAT = new PointblankDiplomat();

    public static void init() {
        ENHANCED_CELERESTIALS_DIPLOMAT.init();
        TACZ_DIPLOMAT.init();
        MUSKETMOD_DIPLOMAT.init();
        POINTBLANK_DIPLOMAT.init();
    }
}
