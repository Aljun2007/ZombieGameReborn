package com.aljun.zombiegamereborn.utils;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomUtils {
    public static final Random RANDOM = new Random();

    private static final RandomPool<Direction> HORIZONTAL_DIRECTIONS =
            RandomPool.builder(Direction.class)
                    .add(Direction.NORTH, 1d)
                    .add(Direction.SOUTH, 1d)
                    .add(Direction.EAST, 1d)
                    .add(Direction.WEST, 1d)
                    .build();

    public static Direction randomHorizontalDirection() {
        return HORIZONTAL_DIRECTIONS.nextValue();
    }

    public static boolean booleanByChance(double chance) {
        if (chance <= 0) return false;
        else if (chance >= 1) return true;
        else return RANDOM.nextDouble(0d, 1d) <= chance;
    }

    public static int nextInt(int min, int max) {
        if (min >= max) {
            return min;
        }
        return RANDOM.nextInt(min, max + 1);
    }

    public static class RandomPool<T> {
        private final List<T> VAR;
        private final List<Double> WEIGHT;
        private final double weightTotal;

        public RandomPool(List<T> var, List<Double> weight, double weightTotal) {
            this.VAR = var;
            this.WEIGHT = weight;
            this.weightTotal = weightTotal;
        }

        public static <T> Builder<T> builder(Class<T> directionClass) {
            return new Builder<>();
        }

        public T nextValue() {
            if (weightTotal==0d) return null;
            double random = RANDOM.nextDouble(0d, weightTotal);
            double before = 0d;
            double after = 0d;
            int i = -1;
            for (double j : WEIGHT) {
                i++;
                after += j;
                if (before <= random && random <= after) return VAR.get(i);
                before += j;
            }
            return VAR.get(0);
        }

        public static class Builder<T> {
            private final List<T> VALUE = new ArrayList<>();
            private final List<Double> WEIGHT = new ArrayList<>();
            private double weightTotal = 0d;

            public RandomPool<T> build() {
                return new RandomPool<>(VALUE, WEIGHT, weightTotal);
            }

            public Builder<T> add(T value, double weight) {
                if (weight > 0) {
                    if (VALUE.contains(value)) {
                        WEIGHT.set(VALUE.indexOf(value), WEIGHT.get(VALUE.indexOf(value)) + weight);
                    } else {
                        VALUE.add(value);
                        WEIGHT.add(weight);
                    }
                    weightTotal += weight;
                } else if (weight < 0) {
                    throw new IndexOutOfBoundsException("\"weight\" > 0d, but :" + weight);
                }
                return this;
            }
        }
    }
}
