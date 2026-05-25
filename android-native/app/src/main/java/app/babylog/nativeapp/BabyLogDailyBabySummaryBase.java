package app.babylog.nativeapp;

class BabyLogDailyBabySummaryBase {
    public final String dateInput;
    public final int feedCount;
    public final int feedTotalMl;
    public final String feedLastTime;
    public final int sleepTotalMinutes;
    public final int sleepIncompleteCount;
    public final int peeCount;
    public final int poopCount;
    public final int diaperCount;
    public final double temperatureMax;
    public final double temperatureMin;
    public final String temperatureLastTime;
    public final String medicationLastName;
    public final String medicationLastTime;
    public final int milestoneCount;

    BabyLogDailyBabySummaryBase(Values values) {
        this.dateInput = values.dateInput == null ? "" : values.dateInput;
        this.feedCount = values.feedCount;
        this.feedTotalMl = values.feedTotalMl;
        this.feedLastTime = values.feedLastTime == null ? "" : values.feedLastTime;
        this.sleepTotalMinutes = values.sleepTotalMinutes;
        this.sleepIncompleteCount = values.sleepIncompleteCount;
        this.peeCount = values.peeCount;
        this.poopCount = values.poopCount;
        this.diaperCount = values.diaperCount;
        this.temperatureMax = values.temperatureMax;
        this.temperatureMin = values.temperatureMin;
        this.temperatureLastTime = values.temperatureLastTime == null ? "" : values.temperatureLastTime;
        this.medicationLastName = values.medicationLastName == null ? "" : values.medicationLastName;
        this.medicationLastTime = values.medicationLastTime == null ? "" : values.medicationLastTime;
        this.milestoneCount = values.milestoneCount;
    }

    static final class Values {
        final String dateInput;
        final int feedCount;
        final int feedTotalMl;
        final String feedLastTime;
        final int sleepTotalMinutes;
        final int sleepIncompleteCount;
        final int peeCount;
        final int poopCount;
        final int diaperCount;
        final double temperatureMax;
        final double temperatureMin;
        final String temperatureLastTime;
        final String medicationLastName;
        final String medicationLastTime;
        final int milestoneCount;

        Values(
                String dateInput,
                int feedCount,
                int feedTotalMl,
                String feedLastTime,
                int sleepTotalMinutes,
                int sleepIncompleteCount,
                int peeCount,
                int poopCount,
                int diaperCount,
                double temperatureMax,
                double temperatureMin,
                String temperatureLastTime,
                String medicationLastName,
                String medicationLastTime,
                int milestoneCount
        ) {
            this.dateInput = dateInput;
            this.feedCount = feedCount;
            this.feedTotalMl = feedTotalMl;
            this.feedLastTime = feedLastTime;
            this.sleepTotalMinutes = sleepTotalMinutes;
            this.sleepIncompleteCount = sleepIncompleteCount;
            this.peeCount = peeCount;
            this.poopCount = poopCount;
            this.diaperCount = diaperCount;
            this.temperatureMax = temperatureMax;
            this.temperatureMin = temperatureMin;
            this.temperatureLastTime = temperatureLastTime;
            this.medicationLastName = medicationLastName;
            this.medicationLastTime = medicationLastTime;
            this.milestoneCount = milestoneCount;
        }
    }
}
