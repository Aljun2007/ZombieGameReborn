package com.aljun.zombiegamereborn.utils;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class RandomUtils {
    public static final Random RANDOM = new Random();

    public static Direction randomHorizontalDirection() {
        int i = RANDOM.nextInt(0,3);
        return switch (i) {
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            case 3 -> Direction.EAST;
            default -> Direction.NORTH;
        };
    }

    // ========== 正态分布路标池 ==========

    /**
     * 正态分布路标池：根据正态分布生成的数值，映射到对应的值
     * 例如：阈值 2.2 对应皮革，3.8 对应金，5.5 对应锁链...
     * 当 tier=3.0 时，因为 3.0 < 3.8 且 >= 2.2，返回金
     */
    public static class NormalRoadPool<T> {
        private final List<T> values;
        private final List<Double> thresholds;
        private final double mu;
        private final double sigma;
        private final T defaultValue;

        /**
         * @param values      按阈值从低到高排列的值列表
         * @param thresholds  对应的阈值列表（必须严格递增）
         * @param mu          正态分布均值
         * @param sigma       正态分布标准差
         * @param defaultValue 当 tier < 第一个阈值时返回的默认值
         */
        public NormalRoadPool(List<T> values, List<Double> thresholds, double mu, double sigma, T defaultValue) {
            this.values = new ArrayList<>(values);
            this.thresholds = new ArrayList<>(thresholds);
            this.mu = mu;
            this.sigma = sigma;
            this.defaultValue = defaultValue;
        }

        /**
         * 根据正态分布生成一个值
         */
        public T nextValue() {
            double tier = mu + sigma * RANDOM.nextGaussian();
            return getValue(tier);
        }

        /**
         * 根据指定的 tier 获取对应的值
         */
        public T getValue(double tier) {
            if (tier < thresholds.get(0)) {
                return defaultValue;
            }
            for (int i = 0; i < thresholds.size(); i++) {
                if (tier < thresholds.get(i)) {
                    return values.get(i);
                }
            }
            return values.get(values.size() - 1);
        }

        /**
         * 获取当前池的分布参数（用于调试）
         */
        public double getMu() { return mu; }
        public double getSigma() { return sigma; }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private final List<T> values = new ArrayList<>();
            private final List<Double> thresholds = new ArrayList<>();
            private double mu = 1.8;
            private double sigma = 1.9;
            private T defaultValue = null;

            public Builder<T> add(T value, double threshold) {
                if (!values.isEmpty() && threshold <= thresholds.get(thresholds.size() - 1)) {
                    throw new IllegalArgumentException("threshold must be strictly increasing");
                }
                values.add(value);
                thresholds.add(threshold);
                return this;
            }

            public Builder<T> mu(double mu) {
                this.mu = mu;
                return this;
            }

            public Builder<T> sigma(double sigma) {
                this.sigma = sigma;
                return this;
            }

            public Builder<T> defaultValue(T defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public NormalRoadPool<T> build() {
                if (values.isEmpty()) {
                    throw new IllegalStateException("No values registered");
                }
                // 根据难度调整 mu 和 sigma（在这里不做动态调整，由调用方控制）
                return new NormalRoadPool<>(values, thresholds, mu, sigma, defaultValue);
            }
        }
    }

    // ========== 原有工具方法 ==========

    public static boolean booleanByChance(double chance) {
        if (chance <= 0) return false;
        else if (chance >= 1) return true;
        else return RANDOM.nextDouble(0d, 1d) <= chance;
    }

    public static boolean booleanByChance(double chance, RandomSource random) {
        if (chance <= 0) return false;
        else if (chance >= 1) return true;
        else return random.nextFloat() < chance;
    }

    public static int nextInt(int min, int max) {
        if (min >= max) return min;
        return RANDOM.nextInt(min, max + 1);
    }

    // ========== RandomPool（原有的） ==========

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
            if (weightTotal == 0d) return null;
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