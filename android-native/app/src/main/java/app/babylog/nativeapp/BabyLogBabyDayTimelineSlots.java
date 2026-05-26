package app.babylog.nativeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class BabyLogBabyDayTimelineSlots {
    static final int MINUTES_PER_DAY = 24 * 60;

    private BabyLogBabyDayTimelineSlots() {
    }

    public static TimelineSlots compute(List<BabyLogDomain.BabyLogEvent> events, String dateInput) {
        String day = BabyLogFormatters.isValidDateInput(dateInput) ? dateInput : BabyLogFormatters.todayDateInput();
        long dayStartMillis = BabyLogFormatters.parseIsoMillis(day + "T00:00:00.000+0800");
        long dayEndMillis = dayStartMillis + MINUTES_PER_DAY * 60_000L;
        List<SleepSegment> sleepSegments = new ArrayList<>();
        List<EventPoint> eventPoints = new ArrayList<>();
        if (events == null) {
            return new TimelineSlots(sleepSegments, eventPoints);
        }
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || !BabyLogFormatters.matchesTimelineFilter(event.eventType, "baby")) {
                continue;
            }
            if ("sleep".equals(event.eventType)) {
                addSleepSegment(event, dayStartMillis, dayEndMillis, sleepSegments);
            } else {
                addEventPoint(event, dayStartMillis, dayEndMillis, eventPoints);
            }
        }
        Collections.sort(sleepSegments, Comparator
                .comparingInt((SleepSegment segment) -> segment.startMinuteOfDay)
                .thenComparingInt(segment -> segment.endMinuteOfDay));
        Collections.sort(eventPoints, Comparator
                .comparingInt((EventPoint point) -> point.minuteOfDay)
                .thenComparing(point -> point.eventId));
        return new TimelineSlots(sleepSegments, eventPoints);
    }

    private static void addSleepSegment(
            BabyLogDomain.BabyLogEvent event,
            long dayStartMillis,
            long dayEndMillis,
            List<SleepSegment> output
    ) {
        String sleepStart = event.payload == null ? "" : event.payload.optString("sleepStart");
        long startMillis = BabyLogFormatters.parseIsoMillis(isBlank(sleepStart) ? event.occurredAt : sleepStart);
        if (startMillis <= 0L) {
            return;
        }
        String sleepEnd = event.payload == null ? "" : event.payload.optString("sleepEnd");
        boolean incomplete = isBlank(sleepEnd);
        long endMillis = incomplete ? dayEndMillis : BabyLogFormatters.parseIsoMillis(sleepEnd);
        if (endMillis <= startMillis) {
            if (incomplete) {
                endMillis = dayEndMillis;
            } else {
                return;
            }
        }
        if (endMillis <= dayStartMillis || startMillis >= dayEndMillis) {
            return;
        }
        long clippedStart = Math.max(startMillis, dayStartMillis);
        long clippedEnd = Math.min(endMillis, dayEndMillis);
        int startMinute = millisToMinuteOfDay(clippedStart, dayStartMillis);
        int endMinute = millisToMinuteOfDay(clippedEnd, dayStartMillis);
        if (endMinute <= startMinute) {
            return;
        }
        output.add(new SleepSegment(
                event.id,
                startMinute,
                endMinute,
                startMillis < dayStartMillis,
                endMillis > dayEndMillis || incomplete,
                incomplete,
                BabyLogFormatters.eventSummary(event)
        ));
    }

    private static void addEventPoint(
            BabyLogDomain.BabyLogEvent event,
            long dayStartMillis,
            long dayEndMillis,
            List<EventPoint> output
    ) {
        long occurredMillis = BabyLogFormatters.parseIsoMillis(event.occurredAt);
        if (occurredMillis < dayStartMillis || occurredMillis >= dayEndMillis) {
            return;
        }
        output.add(new EventPoint(
                event.id,
                event.eventType,
                millisToMinuteOfDay(occurredMillis, dayStartMillis),
                BabyLogFormatters.eventSummary(event)
        ));
    }

    private static int millisToMinuteOfDay(long millis, long dayStartMillis) {
        long raw = (millis - dayStartMillis) / 60_000L;
        return (int) Math.max(0L, Math.min(MINUTES_PER_DAY, raw));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class TimelineSlots {
        public final List<SleepSegment> sleepSegments;
        public final List<EventPoint> eventPoints;

        TimelineSlots(List<SleepSegment> sleepSegments, List<EventPoint> eventPoints) {
            this.sleepSegments = Collections.unmodifiableList(new ArrayList<>(sleepSegments));
            this.eventPoints = Collections.unmodifiableList(new ArrayList<>(eventPoints));
        }
    }

    public static final class SleepSegment {
        public final String eventId;
        public final int startMinuteOfDay;
        public final int endMinuteOfDay;
        public final boolean startsBeforeDay;
        public final boolean endsAfterDay;
        public final boolean incomplete;
        public final String summaryLabel;

        SleepSegment(
                String eventId,
                int startMinuteOfDay,
                int endMinuteOfDay,
                boolean startsBeforeDay,
                boolean endsAfterDay,
                boolean incomplete,
                String summaryLabel
        ) {
            this.eventId = eventId;
            this.startMinuteOfDay = startMinuteOfDay;
            this.endMinuteOfDay = endMinuteOfDay;
            this.startsBeforeDay = startsBeforeDay;
            this.endsAfterDay = endsAfterDay;
            this.incomplete = incomplete;
            this.summaryLabel = summaryLabel == null ? "" : summaryLabel;
        }
    }

    public static final class EventPoint {
        public final String eventId;
        public final String eventType;
        public final int minuteOfDay;
        public final String summaryLabel;

        EventPoint(String eventId, String eventType, int minuteOfDay, String summaryLabel) {
            this.eventId = eventId == null ? "" : eventId;
            this.eventType = eventType == null ? "" : eventType;
            this.minuteOfDay = minuteOfDay;
            this.summaryLabel = summaryLabel == null ? "" : summaryLabel;
        }
    }
}
