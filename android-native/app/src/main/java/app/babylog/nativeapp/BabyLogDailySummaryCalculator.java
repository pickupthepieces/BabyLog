package app.babylog.nativeapp;

import org.json.JSONObject;

import java.util.List;
import java.util.OptionalInt;

final class BabyLogDailySummaryCalculator {
    private BabyLogDailySummaryCalculator() {
    }

    static BabyLogService.DailyBabySummary calculate(List<BabyLogDomain.BabyLogEvent> events, String dateInput) {
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
                || "milestone".equals(eventType);
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
        private String feedLastTime = "";
        private int sleepTotalMinutes;
        private int sleepIncompleteCount;
        private int peeCount;
        private int poopCount;
        private int diaperCount;
        private double temperatureMax = Double.NaN;
        private double temperatureMin = Double.NaN;
        private String temperatureLastTime = "";
        private String medicationLastName = "";
        private String medicationLastTime = "";
        private int milestoneCount;

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
            } else if ("poop".equals(event.eventType)) {
                poopCount += 1;
            } else if ("diaper".equals(event.eventType)) {
                acceptDiaper(payload);
            } else if ("temperature".equals(event.eventType)) {
                acceptTemperature(event, payload);
            } else if ("medication".equals(event.eventType)) {
                acceptMedication(event, payload);
            } else if ("milestone".equals(event.eventType)) {
                milestoneCount += 1;
            }
        }

        private void acceptFeed(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            feedCount += 1;
            Double amount = payloadNumber(payload, "amountMl");
            if (amount != null) {
                feedTotalMl += (int) Math.round(amount);
            }
            if (isNewer(event.occurredAt, feedLastTime)) {
                feedLastTime = event.occurredAt;
            }
        }

        private void acceptSleep(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
            OptionalInt duration = BabyLogService.sleepDurationMinutes(event);
            if (duration.isPresent()) {
                sleepTotalMinutes += duration.getAsInt();
            } else if (!isBlank(payload.optString("sleepStart")) && isBlank(payload.optString("sleepEnd"))) {
                sleepIncompleteCount += 1;
            }
        }

        private void acceptDiaper(JSONObject payload) {
            diaperCount += 1;
            String kind = BabyLogDiaperKind.fromPayload(payload);
            if (BabyLogDiaperKind.PEE.equals(kind) || BabyLogDiaperKind.BOTH.equals(kind)) {
                peeCount += 1;
            }
            if (BabyLogDiaperKind.POOP.equals(kind) || BabyLogDiaperKind.BOTH.equals(kind)) {
                poopCount += 1;
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

        BabyLogService.DailyBabySummary build() {
            return new BabyLogService.DailyBabySummary(new BabyLogDailyBabySummaryBase.Values(
                    dateInput,
                    feedCount,
                    feedTotalMl,
                    feedLastTime,
                    sleepTotalMinutes,
                    sleepIncompleteCount,
                    peeCount,
                    poopCount,
                    diaperCount,
                    temperatureMax,
                    temperatureMin,
                    temperatureLastTime,
                    medicationLastName,
                    medicationLastTime,
                    milestoneCount
            ));
        }
    }
}
