import { createEvent, createSyncChange, EVENT_TYPES } from "../domain/types";
import type { BabyLogEvent, CreateEventInput, EventType } from "../domain/types";
import type { LocalRepository } from "../storage/localRepository";

export type EventDaySummary = {
  counts: Record<EventType, number>;
  lastByType: Partial<Record<EventType, BabyLogEvent>>;
};

export async function recordLocalEvent(
  repository: LocalRepository,
  input: CreateEventInput
): Promise<BabyLogEvent> {
  const event = createEvent(input);
  const change = createSyncChange({
    familyId: event.familyId,
    childId: event.childId,
    entityType: "event",
    entityId: event.id,
    operation: "upsert"
  });

  await repository.put("events", event);
  await repository.put("syncChanges", change);

  return event;
}

export async function listRecentEvents(
  repository: LocalRepository,
  familyId: string,
  limit = 20
): Promise<BabyLogEvent[]> {
  const events = await repository.listByFamily("events", familyId);

  return sortNewestFirst(events).slice(0, limit);
}

export async function summarizeEventDay(
  repository: LocalRepository,
  familyId: string,
  startIso: string,
  endIso: string
): Promise<EventDaySummary> {
  const events = await repository.queryByDate("events", familyId, startIso, endIso);
  const counts = createEmptyEventCounts();
  const lastByType: Partial<Record<EventType, BabyLogEvent>> = {};

  for (const event of sortNewestFirst(events)) {
    counts[event.eventType] += 1;
    lastByType[event.eventType] ??= event;
  }

  return { counts, lastByType };
}

function createEmptyEventCounts(): Record<EventType, number> {
  return Object.fromEntries(EVENT_TYPES.map((eventType) => [eventType, 0])) as Record<EventType, number>;
}

function sortNewestFirst(events: BabyLogEvent[]): BabyLogEvent[] {
  return [...events].sort((left, right) => Date.parse(right.occurredAt) - Date.parse(left.occurredAt));
}
