package com.aljun.zombiegamereborn.diplomat;

import com.aljun.zombiegamereborn.diplomat.enhancedcelestials.EnhancedCelestialsDiplomat;
import com.aljun.zombiegamereborn.diplomat.musketmod.MusketmodDiplomat;
import com.aljun.zombiegamereborn.diplomat.tacz.TACZDiplomat;

public class ZGRDiplomacyCenter {
    public static final EnhancedCelestialsDiplomat ENHANCED_CELERESTIALS_DIPLOMAT = new EnhancedCelestialsDiplomat();
    public static final TACZDiplomat TACZ_DIPLOMAT = new TACZDiplomat();
    public static final MusketmodDiplomat MUSKETMOD_DIPLOMAT = new MusketmodDiplomat();

    public static void init() {
        ENHANCED_CELERESTIALS_DIPLOMAT.init();
        TACZ_DIPLOMAT.init();
        MUSKETMOD_DIPLOMAT.init();
    }
}
