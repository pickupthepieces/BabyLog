import { createAttachmentRecord, createEvent, createSyncChange, EVENT_TYPES } from "../domain/types";
import type { BabyLogEvent, CreateEventInput, EventType } from "../domain/types";
import type { UltrasoundFieldKey } from "../domain/ultrasound";
import { blobToBase64, createAttachmentBlobRecord } from "../storage/attachments";
import type { LocalRepository } from "../storage/localRepository";

export type EventDaySummary = {
  counts: Record<EventType, number>;
  lastByType: Partial<Record<EventType, BabyLogEvent>>;
};

export type LocalUltrasoundFields = Partial<Record<UltrasoundFieldKey, string | number>>;

export type RecordLocalUltrasoundInput = {
  familyId: string;
  childId: string;
  occurredAt: string;
  fields: LocalUltrasoundFields;
  imageFile?: File;
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

export async function recordLocalUltrasound(
  repository: LocalRepository,
  input: RecordLocalUltrasoundInput
): Promise<BabyLogEvent> {
  const attachmentIds: string[] = [];

  if (input.imageFile) {
    const attachment = createAttachmentRecord({
      familyId: input.familyId,
      childId: input.childId,
      kind: "ultrasound_image",
      originalName: input.imageFile.name,
      mimeType: input.imageFile.type || "application/octet-stream",
      byteSize: input.imageFile.size,
      localBlobKey: `blob/${input.familyId}/${Date.now()}-${input.imageFile.name}`
    });
    const blob = createAttachmentBlobRecord({
      familyId: input.familyId,
      attachmentId: attachment.id,
      blob: input.imageFile
    });
    blob.dataBase64 = await blobToBase64(input.imageFile);
    const attachmentChange = createSyncChange({
      familyId: input.familyId,
      childId: input.childId,
      entityType: "attachment",
      entityId: attachment.id,
      operation: "upsert"
    });

    await repository.put("attachments", attachment);
    await repository.put("attachmentBlobs", blob);
    await repository.put("syncChanges", attachmentChange);
    attachmentIds.push(attachment.id);
  }

  return recordLocalEvent(repository, {
    familyId: input.familyId,
    childId: input.childId,
    eventType: "ultrasound",
    occurredAt: input.occurredAt,
    attachmentIds,
    payload: {
      ...input.fields,
      summary: formatUltrasoundSummary(input.fields)
    }
  });
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

function formatUltrasoundSummary(fields: LocalUltrasoundFields): string {
  const parts = [
    typeof fields.gestationalAgeDays === "number" ? formatGestationalAge(fields.gestationalAgeDays) : null,
    typeof fields.efwGram === "number" ? `EFW ${fields.efwGram} g` : null,
    typeof fields.bpdMm === "number" ? `BPD ${fields.bpdMm} mm` : null
  ].filter(Boolean);

  return parts.length > 0 ? parts.join(" · ") : "B 超手动记录 · 待补充指标";
}

function formatGestationalAge(days: number): string {
  const weeks = Math.floor(days / 7);
  const remainingDays = days % 7;

  return `${weeks}+${remainingDays} 周`;
}
