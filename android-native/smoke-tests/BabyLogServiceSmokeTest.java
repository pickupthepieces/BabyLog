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

        assertEquals(
                "产检 · 市妇幼产科 · 一切正常",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.checkup("2026-05-18", "市妇幼产科", "一切正常", "下次 2026-06-16")
                )
        );
        assertEquals(
                "胎动 · 20:00-21:00 · 10 次",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.fetalMovement("20:00-21:00", "10", "饭后")
                )
        );
        assertEquals(
                "宫缩 · 22:10 · 间隔 5 分钟 · 持续 40 秒",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.contraction("22:10", "5", "40", "")
                )
        );
        assertEquals(
                "孕妈指标 · 体重 60.4 kg · 血压 118/76 mmHg · 血糖 空腹 5.2 mmol/L",
                BabyLogService.formatMaternalMetricSummary(
                        BabyLogService.MaternalMetricInput.create("60.4", "118", "76", "5.2", "fasting", "")
                )
        );
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

}
