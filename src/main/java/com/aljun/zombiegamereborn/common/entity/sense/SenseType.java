package com.aljun.zombiegamereborn.common.entity.sense;

import java.util.Objects;

public class SenseType {
    public static final SenseType BLEEDING = new SenseType(64d, 400) ;
    public static final SenseType BLOCK = new SenseType( 16d, 100);
    public static final SenseType GUN_SHOT = new SenseType( 64d, 400);
    public static final SenseType GUN_SHOT_SILENCED = new SenseType( 16d, 100);
    private final int id;
    private static int idTotal = 0;
    double radius;
    int baseLifespan;

    public SenseType( double radius, int baseLifespan) {
        this.id = idTotal++;
        this.radius = radius;
        this.baseLifespan = baseLifespan;
    }

    public int id() {
        return id;
    }

    public double radius() {
        return radius;
    }

    public int baseLifespan() {
        return baseLifespan;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SenseType) obj;
        return this.id == that.id &&
                Double.doubleToLongBits(this.radius) == Double.doubleToLongBits(that.radius) &&
                this.baseLifespan == that.baseLifespan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, radius, baseLifespan);
    }

    @Override
    public String toString() {
        return "SenseType[" +
                "id=" + id + ", " +
                "radius=" + radius + ", " +
                "baseLifespan=" + baseLifespan + ']';
    }

    public void refresh(double radius, int baseLifespan) {
        this.radius = radius;
        this.baseLifespan = baseLifespan;
    }


}
