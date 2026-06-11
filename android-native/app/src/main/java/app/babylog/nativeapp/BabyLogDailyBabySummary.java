package app.babylog.nativeapp;

public final class BabyLogDailyBabySummary {
    public final String dateInput;
    public final int feedCount;
    public final int feedTotalMl;
    public final String feedLastTime;
    public final String feedLastType;
    public final int sleepTotalMinutes;
    public final int sleepIncompleteCount;
    public final int sleepLongestMinutes;
    public final String sleepLastTime;
    public final int peeCount;
    public final int poopCount;
    public final int diaperCount;
    public final String diaperLastKind;
    public final String diaperLastTime;
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
            String feedLastType,
            int sleepTotalMinutes,
            int sleepIncompleteCount,
            int sleepLongestMinutes,
            String sleepLastTime,
            int peeCount,
            int poopCount,
            int diaperCount,
            String diaperLastKind,
            String diaperLastTime,
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
        this.feedLastType = feedLastType == null ? "" : feedLastType;
        this.sleepTotalMinutes = sleepTotalMinutes;
        this.sleepIncompleteCount = sleepIncompleteCount;
        this.sleepLongestMinutes = sleepLongestMinutes;
        this.sleepLastTime = sleepLastTime == null ? "" : sleepLastTime;
        this.peeCount = peeCount;
        this.poopCount = poopCount;
        this.diaperCount = diaperCount;
        this.diaperLastKind = diaperLastKind == null ? "" : diaperLastKind;
        this.diaperLastTime = diaperLastTime == null ? "" : diaperLastTime;
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
