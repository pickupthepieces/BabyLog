import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogDueDateCalculator;

public final class BabyLogDueDateCalculatorSmokeTest {
    public static void main(String[] args) {
        BabyLogDueDateCalculator.LmpResult lmp = BabyLogDueDateCalculator.fromLmp("2026-01-01", 28, "2026-05-20");
        require("2026-10-08".equals(lmp.estimatedDueDate), "LMP EDD should use Naegele + 280 days");
        require(lmp.gestationalAgeDays == 139, "LMP GA days should be days since LMP");
        require("19+6 周".equals(lmp.gestationalAgeLabel), "LMP GA label should format weeks+days");

        BabyLogDueDateCalculator.LmpResult longCycle = BabyLogDueDateCalculator.fromLmp("2026-01-01", 30, "2026-05-20");
        require("2026-10-10".equals(longCycle.estimatedDueDate), "cycle length should shift EDD");

        BabyLogDueDateCalculator.CrlResult crl = BabyLogDueDateCalculator.fromCrl(50.0, "2026-04-01");
        require(crl.valid, "CRL 50mm should be inside supported window");
        require(crl.gestationalAgeDays == 81, "CRL 50mm should round to about 81 GA days");
        require("11+4 周".equals(crl.gestationalAgeLabel), "CRL GA label should format rounded days");
        require("2026-10-17".equals(crl.estimatedDueDate), "CRL EDD should reverse from rounded GA");

        BabyLogDueDateCalculator.CrlResult tooSmall = BabyLogDueDateCalculator.fromCrl(5.0, "2026-04-01");
        require(!tooSmall.valid && tooSmall.message.contains("窗口"), "CRL below window should not calculate");

        BabyLogDueDateCalculator.CrlResult tooLarge = BabyLogDueDateCalculator.fromCrl(100.0, "2026-04-01");
        require(!tooLarge.valid && tooLarge.message.contains("窗口"), "CRL above window should not calculate");

        require(BabyLogDueDateCalculator.diffDays("2026-10-08", "2026-10-17") == 9, "EDD diff should be absolute days");
        require(BabyLogDueDateCalculator.diffDays("2026-10-17", "2026-10-08") == 9, "EDD diff should be symmetric");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
