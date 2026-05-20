package app.babylog.nativeapp;

public final class BabyLogWeightGainCalculator {
    private BabyLogWeightGainCalculator() {
    }

    public static final class Recommendation {
        public final String categoryKey;
        public final String categoryLabel;
        public final double bmi;
        public final double totalGainMinKg;
        public final double totalGainMaxKg;
        public final double weeklyGainKg;

        Recommendation(
                String categoryKey,
                String categoryLabel,
                double bmi,
                double totalGainMinKg,
                double totalGainMaxKg,
                double weeklyGainKg
        ) {
            this.categoryKey = categoryKey;
            this.categoryLabel = categoryLabel;
            this.bmi = bmi;
            this.totalGainMinKg = totalGainMinKg;
            this.totalGainMaxKg = totalGainMaxKg;
            this.weeklyGainKg = weeklyGainKg;
        }

        public String rangeLabel() {
            return BabyLogFormatters.formatNumber(totalGainMinKg) + "–" + BabyLogFormatters.formatNumber(totalGainMaxKg) + " kg";
        }
    }

    public static final class Range {
        public final double minKg;
        public final double maxKg;

        Range(double minKg, double maxKg) {
            this.minKg = minKg;
            this.maxKg = maxKg;
        }

        public String label() {
            return BabyLogFormatters.formatNumber(minKg) + "–" + BabyLogFormatters.formatNumber(maxKg) + " kg";
        }
    }

    public static Recommendation recommendation(double prePregnancyWeightKg, double heightCm) {
        double bmi = bmi(prePregnancyWeightKg, heightCm);
        if (bmi < 18.5) {
            return new Recommendation("underweight", "BMI < 18.5", bmi, 12.5, 18.0, 0.51);
        }
        if (bmi < 25.0) {
            return new Recommendation("normal", "BMI 18.5–24.9", bmi, 11.5, 16.0, 0.42);
        }
        if (bmi < 30.0) {
            return new Recommendation("overweight", "BMI 25.0–29.9", bmi, 7.0, 11.5, 0.28);
        }
        return new Recommendation("obese", "BMI ≥ 30", bmi, 5.0, 9.0, 0.22);
    }

    public static double bmi(double prePregnancyWeightKg, double heightCm) {
        if (prePregnancyWeightKg <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException("weight and height must be positive");
        }
        double heightM = heightCm / 100.0;
        return prePregnancyWeightKg / (heightM * heightM);
    }

    public static Range recommendedGainRangeKg(Recommendation recommendation, int gestationalAgeDays) {
        int clampedDays = Math.max(0, Math.min(280, gestationalAgeDays));
        double weeks = clampedDays / 7.0;
        if (weeks < 13.0) {
            return new Range(0.0, 2.0);
        }
        double progress = Math.min(1.0, Math.max(0.0, (weeks - 13.0) / 27.0));
        return new Range(
                recommendation.totalGainMinKg * progress,
                recommendation.totalGainMaxKg * progress
        );
    }

    public static double cumulativeGainKg(double currentWeightKg, double prePregnancyWeightKg) {
        return currentWeightKg - prePregnancyWeightKg;
    }
}
