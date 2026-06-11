package app.babylog.nativeapp;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

final class BabyLogDailySummaryCalculator {
    private static final String FEED_KIND_BREAST = "breast";
    private static final String FEED_KIND_BOTTLE = "bottle";
    private static final String FEED_KIND_SOLID = "solid";

    private BabyLogDailySummaryCalculator() {
    }

    static BabyLogDailyBabySummary calculate(List<BabyLogDomain.BabyLogEvent> events, String dateInput) {
        String day = isBlank(dateInput) ? BabyLogFormatters.todayDateInput() : dateInput.trim();
        Builder builder = new Builder(day);
        if (events == null) {
            return builder.build();
        }
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || !isDailyBabySummaryEvent(event.eventType) || !day.equals(dailyBabySummaryDay(event))) {
                continue;
            }
            builder.accept(event);
        }
        return builder.build();
    }

    private static boolean isDailyBabySummaryEvent(String eventType) {
        return isFeedSummaryEvent(eventType)
                || "sleep".equals(eventType)
                || "pee".equals(eventType)
                || "poop".equals(eventType)
                || "diaper".equals(eventType)
                || "temperature".equals(eventType)
                || "medication".equals(eventType)
                || "milestone".equals(eventType)
                || "growth".equals(eventType)
                || "child_checkup".equals(eventType);
    }

    private static boolean isFeedSummaryEvent(String eventType) {
        return "feed".equals(eventType) || "breastfeed".equals(eventType) || "bottle".equals(eventType);
    }

    private static String dailyBabySummaryDay(BabyLogDomain.BabyLogEvent event) {
        if ("sleep".equals(event.eventType)) {
            String sleepStart = event.payload == null ? "" : event.payload.optString("sleepStart");
            String sleepStartDay = BabyLogFormatters.recordDay(sleepStart);
            if (BabyLogFormatters.isValidDateInput(sleepStartDay)) {
                return sleepStartDay;
            }
        }
        return BabyLogFormatters.recordDay(event.occurredAt);
    }

    private static Double payloadNumber(JSONObject payload, String key) {
        if (payload == null || !payload.has(key)) {
            return null;
        }
        double value = payload.optDouble(key, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isNewer(String candidate, String current) {
        if (isBlank(candidate)) {
            return false;
        }
        if (isBlank(current)) {
            return true;
        }
        return BabyLogFormatters.parseIsoMillis(candidate) >= BabyLogFormatters.parseIsoMillis(current);
    }

    private static final class Builder {
        private final String dateInput;
        private int feedCount;
        private int feedTotalMl;
        private int feedBreastCount;
        private int feedBottleCount;
        private int feedSolidCount;
        private String feedLastTime = "";
        private String feedLastType = "";
        private int feedLastAmountMl;
        private int sleepTotalMinutes;
        private int sleepIncompleteCount;
        private int sleepLongestMinutes;
        private String sleepLastTime = "";
        private int peeCount;
        private int poopCount;
        private int diaperCount;
        private String diaperLastKind = "";
        private String diaperLastTime = "";
        private double temperatureMax = Double.NaN;
        private double temperatureMin = Double.NaN;
        private String temperatureLastTime = "";
        private String medicationLastName = "";
        private String medicationLastTime = "";
        private int milestoneCount;
        private double growthWeightKg = Double.NaN;
        private double growthHeightCm = Double.NaN;
        private double growthHeadCircumferenceCm = Double.NaN;
        private String growthLastTime = "";

        Builder(String dateInput) {
            this.dateInput = dateInput;
        }

        void accept(BabyLogDomain.BabyLogEvent event) {
            JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
            if (isFeedSummaryEvent(event.eventType)) {
                acceptFeed(event, payload);
            } else if ("sleep".equals(event.eventType)) {
                acceptSleep(event, payload);
            } else if ("pee".equals(event.eventType)) {
                peeCount += 1;
                acceptDiaperQuickEvent(event, BabyLogDiaperKind.PEE);
            } else if ("poop".equals(event.eventType)) {
                poopCount += 1;
                acceptDiaperQuickEvent(event, BabyLogDiaperKind.POOP);
            } else if ("diaper".equals(event.eventType)) {
                acceptDiaper(event, payload);
            } else if ("temperature".equals(event.eventType)) {
                acceptTemperature(event, payload);
            } else if ("medication".equals(event.eventType)) {
                acceptMedication(event, payload);
            } else if ("milestone".equals(event.eventType)) {
                milestoneCount += 1;
            } else if ("growth".equals(event.eventType) || "child_checkup".equals(event.eventType)) {
                acceptGrowth(event, payload);
            }
        }

        private void acceptFeed(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            feedCount += 1;
            Double amount = payloadNumber(payload, "amountMl");
            int roundedAmount = 0;
            if (amount != null) {
                roundedAmount = (int) Math.round(amount);
                feedTotalMl += roundedAmount;
            }
            String feedKind = feedKind(event.eventType, payload);
            if (FEED_KIND_BREAST.equals(feedKind)) {
                feedBreastCount += 1;
            } else if (FEED_KIND_BOTTLE.equals(feedKind)) {
                feedBottleCount += 1;
            } else if (FEED_KIND_SOLID.equals(feedKind)) {
                feedSolidCount += 1;
            }
            if (isNewer(event.occurredAt, feedLastTime)) {
                feedLastTime = event.occurredAt;
                feedLastType = feedTypeLabel(feedKind, event.eventType, payload);
                feedLastAmountMl = roundedAmount;
            }
        }

        private void acceptSleep(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            OptionalInt duration = BabyLogService.sleepDurationMinutes(event);
            if (duration.isPresent()) {
                int minutes = duration.getAsInt();
                sleepTotalMinutes += minutes;
                sleepLongestMinutes = Math.max(sleepLongestMinutes, minutes);
            } else if (!isBlank(payload.optString("sleepStart")) && isBlank(payload.optString("sleepEnd"))) {
                sleepIncompleteCount += 1;
            }
            String marker = firstNonBlank(payload.optString("sleepEnd"), payload.optString("sleepStart"), event.occurredAt);
            if (isNewer(marker, sleepLastTime)) {
                sleepLastTime = marker;
            }
        }

        private void acceptDiaper(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            diaperCount += 1;
            String kind = BabyLogDiaperKind.fromPayload(payload);
            if (BabyLogDiaperKind.PEE.equals(kind) || BabyLogDiaperKind.BOTH.equals(kind)) {
                peeCount += 1;
            }
            if (BabyLogDiaperKind.POOP.equals(kind) || BabyLogDiaperKind.BOTH.equals(kind)) {
                poopCount += 1;
            }
            if (isNewer(event.occurredAt, diaperLastTime)) {
                diaperLastTime = event.occurredAt;
                diaperLastKind = diaperKindLabel(kind);
            }
        }

        private void acceptDiaperQuickEvent(BabyLogDomain.BabyLogEvent event, String kind) {
            if (isNewer(event.occurredAt, diaperLastTime)) {
                diaperLastTime = event.occurredAt;
                diaperLastKind = diaperKindLabel(kind);
            }
        }

        private void acceptTemperature(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            Double value = payloadNumber(payload, "temperatureC");
            if (value == null) {
                return;
            }
            temperatureMax = Double.isNaN(temperatureMax) ? value : Math.max(temperatureMax, value);
            temperatureMin = Double.isNaN(temperatureMin) ? value : Math.min(temperatureMin, value);
            if (isNewer(event.occurredAt, temperatureLastTime)) {
                temperatureLastTime = event.occurredAt;
            }
        }

        private void acceptMedication(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            if (isNewer(event.occurredAt, medicationLastTime)) {
                medicationLastTime = event.occurredAt;
                medicationLastName = payload.optString("medicationName");
            }
        }

        private void acceptGrowth(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            if (!isNewer(event.occurredAt, growthLastTime)) {
                return;
            }
            Double weight = payloadNumber(payload, "weightKg");
            Double height = payloadNumber(payload, "heightCm");
            Double head = payloadNumber(payload, "headCircumferenceCm");
            if (weight == null && height == null && head == null) {
                return;
            }
            growthLastTime = event.occurredAt;
            growthWeightKg = weight == null ? Double.NaN : weight;
            growthHeightCm = height == null ? Double.NaN : height;
            growthHeadCircumferenceCm = head == null ? Double.NaN : head;
        }

        BabyLogDailyBabySummary build() {
            return new BabyLogDailyBabySummary(
                    dateInput,
                    feedCount,
                    feedTotalMl,
                    feedBreastCount,
                    feedBottleCount,
                    feedSolidCount,
                    feedLastTime,
                    feedLastType,
                    feedLastAmountMl,
                    sleepTotalMinutes,
                    sleepIncompleteCount,
                    sleepLongestMinutes,
                    sleepLastTime,
                    peeCount,
                    poopCount,
                    diaperCount,
                    diaperLastKind,
                    diaperLastTime,
                    temperatureMax,
                    temperatureMin,
                    temperatureLastTime,
                    medicationLastName,
                    medicationLastTime,
                    milestoneCount,
                    growthWeightKg,
                    growthHeightCm,
                    growthHeadCircumferenceCm,
                    growthLastTime
            );
        }

        private static String feedKind(String eventType, JSONObject payload) {
            if ("bottle".equals(eventType)) {
                return FEED_KIND_BOTTLE;
            }
            if ("breastfeed".equals(eventType)) {
                return FEED_KIND_BREAST;
            }
            String feedType = payload.optString("feedType");
            String normalized = feedType.trim().toLowerCase(Locale.ROOT);
            if ("bottle".equals(normalized) || feedType.contains("奶瓶")) {
                return FEED_KIND_BOTTLE;
            }
            if ("breast".equals(normalized) || feedType.contains("母乳")) {
                return FEED_KIND_BREAST;
            }
            if ("food".equals(normalized) || "solid".equals(normalized) || feedType.contains("辅")) {
                return FEED_KIND_SOLID;
            }
            return "";
        }

        private static String feedTypeLabel(String feedKind, String eventType, JSONObject payload) {
            if (FEED_KIND_BOTTLE.equals(feedKind)) {
                return "奶瓶";
            }
            if (FEED_KIND_BREAST.equals(feedKind)) {
                return "母乳";
            }
            if (FEED_KIND_SOLID.equals(feedKind)) {
                return "辅食";
            }
            String feedType = payload.optString("feedType");
            return isBlank(feedType) ? BabyLogFormatters.eventLabel(eventType) : feedType;
        }

        private static String diaperKindLabel(String kind) {
            if (BabyLogDiaperKind.PEE.equals(kind)) {
                return "尿";
            }
            if (BabyLogDiaperKind.POOP.equals(kind)) {
                return "便";
            }
            if (BabyLogDiaperKind.BOTH.equals(kind)) {
                return "混合";
            }
            return "尿布";
        }

        private static String firstNonBlank(String... values) {
            if (values == null) {
                return "";
            }
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
            return "";
        }
    }
}
