package app.babylog.nativeapp;

import java.util.Locale;

public final class BabyLogFetalGrowthReference {
    public static final String STANDARD_HONG_KONG = "hong_kong_chinese";
    public static final String STANDARD_NICHD_ASIAN = "nichd_asian";
    public static final String STANDARD_CHINA_CUSTOM = "china_custom";
    public static final String STANDARD_HADLOCK = "hadlock";
    public static final String STANDARD_INTERGROWTH = "intergrowth21st";
    public static final String DEFAULT_STANDARD = STANDARD_HONG_KONG;

    private static final String HONG_KONG_LABEL = "香港近似参考";
    private static final String APPROXIMATION_NOTICE = "未校准近似参考：当前百分位/Z-score 由公式和靶值拟合，尚未接入完整参数；请按报告原文复核。";
    private static final int FIRST_WEEK = 14;
    private static final int LAST_WEEK = 40;
    private static final double Z10 = -1.2815515655446004;
    private static final double Z90 = 1.2815515655446004;

    private static final double EFW_SCREENSHOT_SD_LN = 0.10;
    private static final double EFW_EARLY_CORRECTION_INTERCEPT = 0.39809420215921725;
    private static final double EFW_EARLY_CORRECTION_SLOPE = -0.018369657662287653;

    private static final Metric EFW = new Metric("efwGram", "EFW", "g", Scale.LOG_GRAM);
    private static final Metric BPD = new Metric("bpdMm", "BPD", "mm", Scale.CM);
    private static final Metric HC = new Metric("hcMm", "HC", "mm", Scale.CM);
    private static final Metric AC = new Metric("acMm", "AC", "mm", Scale.CM);
    private static final Metric FL = new Metric("flMm", "FL", "mm", Scale.CM);

    private BabyLogFetalGrowthReference() {
    }

    public static Result evaluate(String standardId, String metricKey, int gestationalAgeDays, double value) {
        Metric metric = metricForKey(metricKey);
        if (metric == null || value <= 0 || gestationalAgeDays < FIRST_WEEK * 7 || gestationalAgeDays > LAST_WEEK * 7 + 6) {
            return null;
        }
        double z = zScore(metric, gestationalAgeDays, value);
        if (Double.isNaN(z) || Double.isInfinite(z)) {
            return null;
        }
        double percentile = clamp(normalCdf(z) * 100.0, 0.1, 99.9);
        return new Result(
                STANDARD_HONG_KONG,
                HONG_KONG_LABEL,
                metric.key,
                metric.label,
                metric.unit,
                gestationalAgeDays,
                value,
                referenceValue(metric, gestationalAgeDays, 0),
                percentile,
                z,
                referenceValue(metric, gestationalAgeDays, Z10),
                referenceValue(metric, gestationalAgeDays, 0),
                referenceValue(metric, gestationalAgeDays, Z90)
        );
    }

    public static Double estimateEfwHadlock3Gram(Double bpdMm, Double acMm, Double flMm) {
        if (bpdMm == null || acMm == null || flMm == null || bpdMm <= 0 || acMm <= 0 || flMm <= 0) {
            return null;
        }
        return estimateEfwHadlock3Gram(bpdMm.doubleValue(), acMm.doubleValue(), flMm.doubleValue());
    }

    public static double estimateEfwHadlock3Gram(double bpdMm, double acMm, double flMm) {
        if (bpdMm <= 0 || acMm <= 0 || flMm <= 0) {
            return Double.NaN;
        }
        double bpdCm = bpdMm / 10.0;
        double acCm = acMm / 10.0;
        double flCm = flMm / 10.0;
        double log10 = 1.335
                - 0.0034 * acCm * flCm
                + 0.0316 * bpdCm
                + 0.0457 * acCm
                + 0.1623 * flCm;
        return Math.pow(10.0, log10);
    }

    public static double referenceValue(String standardId, String metricKey, int gestationalAgeDays, double zScore) {
        Metric metric = metricForKey(metricKey);
        if (metric == null || gestationalAgeDays < FIRST_WEEK * 7 || gestationalAgeDays > LAST_WEEK * 7 + 6) {
            return Double.NaN;
        }
        return referenceValue(metric, gestationalAgeDays, zScore);
    }

    public static String formatResult(Result result) {
        if (result == null) {
            return "";
        }
        return "P" + formatNumber(result.percentile) + " · Z " + formatSigned(result.zScore);
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    public static String formatSigned(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        return String.format(Locale.US, "%+.2f", value);
    }

    public static String standardLabel(String standardId) {
        return HONG_KONG_LABEL;
    }

    public static String approximationNotice() {
        return APPROXIMATION_NOTICE;
    }

    private static double zScore(Metric metric, int gestationalAgeDays, double value) {
        double week = gestationalAgeDays / 7.0;
        if (metric.scale == Scale.LOG_GRAM) {
            return (Math.log(value) - efwMeanLn(week)) / EFW_SCREENSHOT_SD_LN;
        }
        double observedCm = value / 10.0;
        double meanCm = biometricMeanCm(metric, week);
        double sigma = biometricSigma(metric, week);
        double lambda = biometricLambda(metric, week);
        if (observedCm <= 0 || meanCm <= 0 || sigma <= 0) {
            return Double.NaN;
        }
        return bccgZ(observedCm, meanCm, sigma, lambda);
    }

    private static double referenceValue(Metric metric, int gestationalAgeDays, double zScore) {
        double week = gestationalAgeDays / 7.0;
        if (metric.scale == Scale.LOG_GRAM) {
            return Math.exp(efwMeanLn(week) + EFW_SCREENSHOT_SD_LN * zScore);
        }
        double meanCm = biometricMeanCm(metric, week);
        double sigma = biometricSigma(metric, week);
        double lambda = biometricLambda(metric, week);
        double referenceCm = bccgReference(meanCm, sigma, lambda, zScore);
        return referenceCm * 10.0;
    }

    private static double efwMeanLn(double week) {
        double base = 0.53506422 + 0.33308619 * week - 0.00361885 * week * week;
        double earlyCorrection = EFW_EARLY_CORRECTION_INTERCEPT + EFW_EARLY_CORRECTION_SLOPE * week;
        return base + Math.max(0, earlyCorrection);
    }

    private static double biometricMeanCm(Metric metric, double week) {
        if (metric == BPD) {
            return -1.32598264 + 0.20385472 * week + 0.00797103 * week * week - 0.00016003 * week * week * week;
        }
        if (metric == HC) {
            return -5.22260453 + 0.75749674 * week + 0.03059762 * week * week - 0.000642244 * week * week * week;
        }
        if (metric == AC) {
            return -9.67592919 + 1.27599572 * week - 0.00011240 * week * week * week;
        }
        if (metric == FL) {
            return -3.36012997 + 0.340450504 * week - 0.000048407 * week * week * week;
        }
        return Double.NaN;
    }

    private static double biometricSigma(Metric metric, double week) {
        if (metric == BPD) {
            return Math.exp(-2.03312605 - 0.03847377 * week);
        }
        if (metric == HC) {
            return Math.exp(-2.18218446 - 0.04131363 * week);
        }
        if (metric == AC) {
            return Math.exp(-2.65372675 - 0.01378611 * week);
        }
        if (metric == FL) {
            return Math.exp(-2.13924086 - 0.03249961 * week);
        }
        return Double.NaN;
    }

    private static double biometricLambda(Metric metric, double week) {
        if (metric == BPD) {
            return 2.45191633;
        }
        if (metric == HC) {
            return 6.55979294 - 0.22558751 * week;
        }
        if (metric == AC) {
            return -0.20107911;
        }
        if (metric == FL) {
            return 0.817961264;
        }
        return Double.NaN;
    }

    private static double bccgZ(double value, double mean, double sigma, double lambda) {
        if (Math.abs(lambda) < 0.000001) {
            return Math.log(value / mean) / sigma;
        }
        return (Math.pow(value / mean, lambda) - 1.0) / (lambda * sigma);
    }

    private static double bccgReference(double mean, double sigma, double lambda, double zScore) {
        if (Math.abs(lambda) < 0.000001) {
            return mean * Math.exp(sigma * zScore);
        }
        double base = 1.0 + lambda * sigma * zScore;
        if (base <= 0) {
            return Double.NaN;
        }
        return mean * Math.pow(base, 1.0 / lambda);
    }

    private static Metric metricForKey(String key) {
        if ("efwGram".equals(key)) return EFW;
        if ("bpdMm".equals(key)) return BPD;
        if ("hcMm".equals(key)) return HC;
        if ("acMm".equals(key)) return AC;
        if ("flMm".equals(key)) return FL;
        return null;
    }

    private static double normalCdf(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    private static double erf(double x) {
        double sign = x < 0 ? -1.0 : 1.0;
        double abs = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * abs);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-abs * abs);
        return sign * y;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    private enum Scale {
        LOG_GRAM,
        CM
    }

    private static final class Metric {
        final String key;
        final String label;
        final String unit;
        final Scale scale;

        Metric(String key, String label, String unit, Scale scale) {
            this.key = key;
            this.label = label;
            this.unit = unit;
            this.scale = scale;
        }
    }

    public static final class Result {
        public final String standardId;
        public final String standardLabel;
        public final String metricKey;
        public final String metricLabel;
        public final String unit;
        public final int gestationalAgeDays;
        public final double value;
        public final double median;
        public final double percentile;
        public final double zScore;
        public final double p10;
        public final double p50;
        public final double p90;

        Result(
                String standardId,
                String standardLabel,
                String metricKey,
                String metricLabel,
                String unit,
                int gestationalAgeDays,
                double value,
                double median,
                double percentile,
                double zScore,
                double p10,
                double p50,
                double p90
        ) {
            this.standardId = standardId;
            this.standardLabel = standardLabel;
            this.metricKey = metricKey;
            this.metricLabel = metricLabel;
            this.unit = unit;
            this.gestationalAgeDays = gestationalAgeDays;
            this.value = value;
            this.median = median;
            this.percentile = percentile;
            this.zScore = zScore;
            this.p10 = p10;
            this.p50 = p50;
            this.p90 = p90;
        }
    }
}
