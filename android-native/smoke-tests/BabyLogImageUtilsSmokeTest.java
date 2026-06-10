import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogImageUtils;

public final class BabyLogImageUtilsSmokeTest {
    public static void main(String[] args) {
        assertEquals(1, BabyLogImageUtils.calculateInSampleSize(1600, 1200, 2048));
        assertEquals(2, BabyLogImageUtils.calculateInSampleSize(4000, 3000, 2048));
        assertEquals(4, BabyLogImageUtils.calculateInSampleSize(8000, 6000, 2048));
        assertEquals(1, BabyLogImageUtils.calculateInSampleSize(0, 6000, 2048));
        assertEquals(1, BabyLogImageUtils.calculateInSampleSize(8000, 6000, 0));
    }

}
