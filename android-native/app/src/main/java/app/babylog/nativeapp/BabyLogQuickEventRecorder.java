package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BabyLogQuickEventRecorder {
    private static final long OPEN_SLEEP_LOOKBACK_MS = 24L * 60L * 60L * 1000L;
    private static final String CLOSED_SLEEP_NOTE = "已闭合睡眠段";

    private BabyLogQuickEventRecorder() {}

    static BabyLogDomain.BabyLogEvent record(
            BabyLogRepository repository,
            BabyLogService.QuickAction action
    ) throws JSONException, BabyLogException {
        String occurredAt = BabyLogFormatters.nowIso();
        BabyLogDomain.BabyLogEvent sleepToClose = "wake".equals(action.eventType)
                ? findLatestOpenSleepEvent(repository, occurredAt)
                : null;
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                action.eventType,
                occurredAt,
                buildPayload(action, occurredAt, sleepToClose != null),
                Collections.emptyList(),
                "manual"
        );
        if (sleepToClose != null) {
            saveWakeWithClosedSleep(repository, event, closeSleepEvent(sleepToClose, occurredAt));
            return event;
        }
        BabyLogDomain.ChildProfile profileUpdate = "birth".equals(event.eventType)
                ? BabyLogService.withBirthDateFromBirthEvent(repository.loadChildProfile(), event.occurredAt)
                : null;
        List<BabyLogDomain.SyncChange> changes = BabyLogService.createSyncChangesForEventUpsert(
                event,
                Collections.emptyList(),
                profileUpdate
        );
        if (!repository.putEventProfileAttachmentsAndSyncChanges(event, profileUpdate, Collections.emptyList(), changes)) {
            throw new BabyLogException.StorageException("保存快捷记录失败");
        }
        return event;
    }

    private static JSONObject buildPayload(
            BabyLogService.QuickAction action,
            String occurredAt,
        boolean closedSleep
    ) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("quickAction", action.label);
        if ("sleep".equals(action.eventType)) {
            payload.put("sleepStart", occurredAt);
        }
        if (closedSleep) {
            payload.put("note", CLOSED_SLEEP_NOTE);
        }
        return payload;
    }

    private static void saveWakeWithClosedSleep(
            BabyLogRepository repository,
            BabyLogDomain.BabyLogEvent wakeEvent,
            BabyLogDomain.BabyLogEvent closedSleep
    ) throws JSONException, BabyLogException {
        List<BabyLogDomain.BabyLogEvent> events = new ArrayList<>();
        events.add(closedSleep);
        events.add(wakeEvent);
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        changes.add(BabyLogDomain.createSyncChange("event", closedSleep.id, "upsert"));
        changes.add(BabyLogDomain.createSyncChange("event", wakeEvent.id, "upsert"));
        if (!repository.putEventsWithSyncChanges(events, changes)) {
            throw new BabyLogException.StorageException("保存快捷记录失败");
        }
    }

    private static BabyLogDomain.BabyLogEvent findLatestOpenSleepEvent(
            BabyLogRepository repository,
            String wakeOccurredAt
    ) {
        long wakeMillis = BabyLogFormatters.parseIsoMillis(wakeOccurredAt);
        if (wakeMillis <= 0L) {
            return null;
        }
        long earliestMillis = wakeMillis - OPEN_SLEEP_LOOKBACK_MS;
        BabyLogDomain.BabyLogEvent latest = null;
        long latestMillis = 0L;
        for (BabyLogDomain.BabyLogEvent event : repository.listEvents()) {
            long startMillis = openSleepStartMillis(event);
            if (startMillis < earliestMillis || startMillis > wakeMillis || startMillis <= latestMillis) {
                continue;
            }
            latest = event;
            latestMillis = startMillis;
        }
        return latest;
    }

    private static long openSleepStartMillis(BabyLogDomain.BabyLogEvent event) {
        if (event == null || !"sleep".equals(event.eventType) || event.payload == null) {
            return 0L;
        }
        String sleepStart = event.payload.optString("sleepStart");
        String sleepEnd = event.payload.optString("sleepEnd");
        if (isBlank(sleepStart) || !isBlank(sleepEnd)) {
            return 0L;
        }
        long startMillis = BabyLogFormatters.parseIsoMillis(sleepStart);
        return startMillis > 0L ? startMillis : BabyLogFormatters.parseIsoMillis(event.occurredAt);
    }

    private static BabyLogDomain.BabyLogEvent closeSleepEvent(
            BabyLogDomain.BabyLogEvent sleepEvent,
            String sleepEnd
    ) throws JSONException, BabyLogException {
        JSONObject payload = sleepEvent.payload == null
                ? new JSONObject()
                : new JSONObject(sleepEvent.payload.toString());
        payload.put("sleepEnd", sleepEnd);
        return BabyLogService.createEditedEvent(sleepEvent, "sleep", payload, sleepEvent.attachmentIds);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
