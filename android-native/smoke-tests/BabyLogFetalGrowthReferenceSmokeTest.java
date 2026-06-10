import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogFetalGrowthReference;

public final class BabyLogFetalGrowthReferenceSmokeTest {
    public static void main(String[] args) {
        assertBetween(128.0, 140.0, BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(33, 106, 17));
        assertBetween(338.0, 350.0, BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(45, 158, 31));

        BabyLogFetalGrowthReference.Result p46 = BabyLogFetalGrowthReference.evaluate(
                "hong_kong_chinese",
                "efwGram",
                20 * 7 + 3,
                344
        );
        assertBetween(44.5, 46.5, p46.percentile);
        assertBetween(-0.16, -0.07, p46.zScore);
        assertEquals("香港近似参考", p46.standardLabel);
        assertEquals("EFW", p46.metricLabel);
        assertTrue(BabyLogFetalGrowthReference.approximationNotice().contains("未校准"));

        BabyLogFetalGrowthReference.Result p5 = BabyLogFetalGrowthReference.evaluate(
                "hong_kong_chinese",
                "efwGram",
                16 * 7 + 5,
                134
        );
        assertBetween(0.15, 0.30, p5.percentile);
        assertBetween(-2.90, -2.79, p5.zScore);

        BabyLogFetalGrowthReference.Result hk = BabyLogFetalGrowthReference.evaluate(
                "hong_kong_chinese",
                "bpdMm",
                20 * 7 + 3,
                45
        );
        assertEquals("香港近似参考", hk.standardLabel);
        assertTrue(hk.percentile > 0 && hk.percentile < 100);
        assertEquals("P" + BabyLogFetalGrowthReference.formatNumber(hk.percentile)
                + " · Z " + BabyLogFetalGrowthReference.formatSigned(hk.zScore),
                BabyLogFetalGrowthReference.formatResult(hk));

        BabyLogFetalGrowthReference.Result ignoredFutureStandard = BabyLogFetalGrowthReference.evaluate(
                "nichd_asian",
                "efwGram",
                20 * 7 + 3,
                344
        );
        assertEquals("hong_kong_chinese", ignoredFutureStandard.standardId);
        assertEquals("香港近似参考", ignoredFutureStandard.standardLabel);
    }



}
