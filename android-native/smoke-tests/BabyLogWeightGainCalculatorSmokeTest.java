import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogWeightGainCalculator;

public final class BabyLogWeightGainCalculatorSmokeTest {
    public static void main(String[] args) {
        BabyLogWeightGainCalculator.Recommendation normal =
                BabyLogWeightGainCalculator.recommendation(55.0, 165.0);
        assertEquals("normal", normal.categoryKey);
        assertNear(20.2, normal.bmi, 0.1);
        assertNear(11.5, normal.totalGainMinKg, 0.01);
        assertNear(16.0, normal.totalGainMaxKg, 0.01);

        BabyLogWeightGainCalculator.Range early =
                BabyLogWeightGainCalculator.recommendedGainRangeKg(normal, 10 * 7);
        assertNear(0.0, early.minKg, 0.01);
        assertNear(2.0, early.maxKg, 0.01);

        BabyLogWeightGainCalculator.Range mid =
                BabyLogWeightGainCalculator.recommendedGainRangeKg(normal, 26 * 7);
        assertNear(5.54, mid.minKg, 0.05);
        assertNear(7.70, mid.maxKg, 0.05);

        BabyLogWeightGainCalculator.Recommendation underweight =
                BabyLogWeightGainCalculator.recommendation(48.0, 168.0);
        assertEquals("underweight", underweight.categoryKey);
        assertNear(12.5, underweight.totalGainMinKg, 0.01);

        BabyLogWeightGainCalculator.Recommendation overweight =
                BabyLogWeightGainCalculator.recommendation(72.0, 165.0);
        assertEquals("overweight", overweight.categoryKey);
        assertNear(7.0, overweight.totalGainMinKg, 0.01);

        BabyLogWeightGainCalculator.Recommendation obese =
                BabyLogWeightGainCalculator.recommendation(86.0, 165.0);
        assertEquals("obese", obese.categoryKey);
        assertNear(5.0, obese.totalGainMinKg, 0.01);

        assertNear(6.2, BabyLogWeightGainCalculator.cumulativeGainKg(61.2, 55.0), 0.01);
    }


}
