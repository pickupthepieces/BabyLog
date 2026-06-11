package app.babylog.nativeapp;

public final class BabyLogDailyBabySummary {
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
    public final double growthWeightKg;
    public final double growthHeightCm;
    public final double growthHeadCircumferenceCm;
    public final String growthLastTime;

    public BabyLogDailyBabySummary(
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
            int milestoneCount,
            double growthWeightKg,
            double growthHeightCm,
            double growthHeadCircumferenceCm,
            String growthLastTime
    ) {
        this.dateInput = dateInput == null ? "" : dateInput;
        this.feedCount = feedCount;
        this.feedTotalMl = feedTotalMl;
        this.feedLastTime = feedLastTime == null ? "" : feedLastTime;
        this.sleepTotalMinutes = sleepTotalMinutes;
        this.sleepIncompleteCount = sleepIncompleteCount;
        this.peeCount = peeCount;
        this.poopCount = poopCount;
        this.diaperCount = diaperCount;
        this.temperatureMax = temperatureMax;
        this.temperatureMin = temperatureMin;
        this.temperatureLastTime = temperatureLastTime == null ? "" : temperatureLastTime;
        this.medicationLastName = medicationLastName == null ? "" : medicationLastName;
        this.medicationLastTime = medicationLastTime == null ? "" : medicationLastTime;
        this.milestoneCount = milestoneCount;
        this.growthWeightKg = growthWeightKg;
        this.growthHeightCm = growthHeightCm;
        this.growthHeadCircumferenceCm = growthHeadCircumferenceCm;
        this.growthLastTime = growthLastTime == null ? "" : growthLastTime;
    }
}
