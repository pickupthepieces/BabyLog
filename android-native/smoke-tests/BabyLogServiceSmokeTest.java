import app.babylog.nativeapp.BabyLogService;

public final class BabyLogServiceSmokeTest {
    public static void main(String[] args) {
        assertEquals(
                "喂养 · 奶瓶 · 120 ml",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.feed("奶瓶", "120", "夜奶")
                )
        );

        assertEquals(
                "睡眠 · 22:10-01:20 · 卧室",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.sleep("22:10", "01:20", "卧室", "")
                )
        );

        assertEquals(
                "尿布 · 便 · 黄色偏稀",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.diaper("便", "黄色偏稀", "")
                )
        );

        assertEquals(
                "体温 · 37.8 ℃ · 腋温",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.temperature("37.8", "腋温", "")
                )
        );

        assertEquals(
                "用药 · 布洛芬 · 2 ml · 发热",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.medication("布洛芬", "2 ml", "发热")
                )
        );
        assertEquals(
                "喂养 · 待补充详情",
                BabyLogService.formatBabyCareSummary(BabyLogService.BabyCareInput.feed("", "", ""))
        );
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

}
