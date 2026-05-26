import app.babylog.nativeapp.BabyLogBabyDayTimelineSlots;
import app.babylog.nativeapp.BabyLogDomain;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;

public final class BabyLogBabyDayTimelineSlotsSmokeTest {
    public static void main(String[] args) throws Exception {
        emptyDayHasNoSlots();
        singleFeedUsesOccurredMinute();
        crossDaySleepShowsTailOnStartDay();
        crossDaySleepShowsHeadOnNextDay();
        openSleepEndsAtDayBoundary();
        mixedEventsAreSorted();
        pregnancyEventsDoNotPolluteBabyTimeline();
        System.out.println("BabyLogBabyDayTimelineSlotsSmokeTest PASS");
    }

    private static void emptyDayHasNoSlots() {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots =
                BabyLogBabyDayTimelineSlots.compute(Collections.emptyList(), "2026-05-25");
        assertEquals(0, slots.sleepSegments.size());
        assertEquals(0, slots.eventPoints.size());
    }

    private static void singleFeedUsesOccurredMinute() {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Collections.singletonList(event("feed", "2026-05-25T06:15:00.000+0800", new JSONObject())),
                "2026-05-25"
        );
        assertEquals(0, slots.sleepSegments.size());
        assertEquals(1, slots.eventPoints.size());
        assertEquals(375, slots.eventPoints.get(0).minuteOfDay);
        assertEquals("feed", slots.eventPoints.get(0).eventType);
    }

    private static void crossDaySleepShowsTailOnStartDay() throws Exception {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Collections.singletonList(sleep("2026-05-25T22:00:00.000+0800", "2026-05-26T05:30:00.000+0800")),
                "2026-05-25"
        );
        assertEquals(1, slots.sleepSegments.size());
        BabyLogBabyDayTimelineSlots.SleepSegment segment = slots.sleepSegments.get(0);
        assertEquals(1320, segment.startMinuteOfDay);
        assertEquals(1440, segment.endMinuteOfDay);
        assertFalse(segment.startsBeforeDay);
        assertTrue(segment.endsAfterDay);
        assertFalse(segment.incomplete);
    }

    private static void crossDaySleepShowsHeadOnNextDay() throws Exception {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Collections.singletonList(sleep("2026-05-25T22:00:00.000+0800", "2026-05-26T05:30:00.000+0800")),
                "2026-05-26"
        );
        assertEquals(1, slots.sleepSegments.size());
        BabyLogBabyDayTimelineSlots.SleepSegment segment = slots.sleepSegments.get(0);
        assertEquals(0, segment.startMinuteOfDay);
        assertEquals(330, segment.endMinuteOfDay);
        assertTrue(segment.startsBeforeDay);
        assertFalse(segment.endsAfterDay);
        assertFalse(segment.incomplete);
    }

    private static void openSleepEndsAtDayBoundary() throws Exception {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Collections.singletonList(sleep("2026-05-25T22:00:00.000+0800", "")),
                "2026-05-25"
        );
        assertEquals(1, slots.sleepSegments.size());
        BabyLogBabyDayTimelineSlots.SleepSegment segment = slots.sleepSegments.get(0);
        assertEquals(1320, segment.startMinuteOfDay);
        assertEquals(1440, segment.endMinuteOfDay);
        assertTrue(segment.endsAfterDay);
        assertTrue(segment.incomplete);
    }

    private static void mixedEventsAreSorted() {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Arrays.asList(
                        event("poop", "2026-05-25T15:00:00.000+0800", new JSONObject()),
                        event("feed", "2026-05-25T06:15:00.000+0800", new JSONObject()),
                        event("temperature", "2026-05-25T09:30:00.000+0800", new JSONObject())
                ),
                "2026-05-25"
        );
        assertEquals(3, slots.eventPoints.size());
        assertEquals(375, slots.eventPoints.get(0).minuteOfDay);
        assertEquals(570, slots.eventPoints.get(1).minuteOfDay);
        assertEquals(900, slots.eventPoints.get(2).minuteOfDay);
    }

    private static void pregnancyEventsDoNotPolluteBabyTimeline() {
        BabyLogBabyDayTimelineSlots.TimelineSlots slots = BabyLogBabyDayTimelineSlots.compute(
                Collections.singletonList(event("ultrasound", "2026-05-25T12:00:00.000+0800", new JSONObject())),
                "2026-05-25"
        );
        assertEquals(0, slots.sleepSegments.size());
        assertEquals(0, slots.eventPoints.size());
    }

    private static BabyLogDomain.BabyLogEvent sleep(String startIso, String endIso) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("sleepStart", startIso);
        if (endIso != null && !endIso.isEmpty()) {
            payload.put("sleepEnd", endIso);
        }
        return event("sleep", startIso, payload);
    }

    private static BabyLogDomain.BabyLogEvent event(String eventType, String occurredAt, JSONObject payload) {
        return BabyLogDomain.createEvent(eventType, occurredAt, payload, Collections.emptyList(), "manual");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }
}
