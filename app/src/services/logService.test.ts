import { beforeEach, describe, expect, it } from "vitest";
import { createEvent } from "../domain/types";
import { createLocalRepository } from "../storage/localRepository";
import { listPendingSyncChanges } from "../storage/syncQueue";
import { listRecentEvents, recordLocalEvent, recordLocalUltrasound, summarizeEventDay } from "./logService";

describe("local log service", () => {
  beforeEach(() => {
    indexedDB.deleteDatabase("babylog-service-test");
  });

  it("records an event and enqueues a future sync change", async () => {
    const repository = createLocalRepository("babylog-service-test");

    const event = await recordLocalEvent(repository, {
      familyId: "family-1",
      childId: "child-1",
      eventType: "feed",
      occurredAt: "2026-05-15T06:00:00+08:00",
      payload: { amountMl: 90 }
    });

    await expect(repository.get("events", event.id)).resolves.toMatchObject({
      eventType: "feed",
      payload: { amountMl: 90 }
    });
    await expect(listPendingSyncChanges(repository, "family-1")).resolves.toMatchObject([
      {
        entityType: "event",
        entityId: event.id,
        operation: "upsert",
        status: "pending"
      }
    ]);
  });

  it("records ultrasound measurements with an optional local image attachment", async () => {
    const repository = createLocalRepository("babylog-service-test");
    const image = new File(["scan-image"], "scan.jpg", { type: "image/jpeg" });

    const event = await recordLocalUltrasound(repository, {
      familyId: "family-1",
      childId: "child-1",
      occurredAt: "2026-05-15T09:30:00+08:00",
      fields: {
        examDate: "2026-05-15",
        gestationalAgeDays: 199,
        bpdMm: 71,
        hcMm: 258,
        acMm: 235,
        flMm: 54,
        efwGram: 1320
      },
      imageFile: image
    });

    const attachments = await repository.listByFamily("attachments", "family-1");
    const savedBlob = await repository.get("attachmentBlobs", `blob_${attachments[0].id}`);
    const pendingChanges = await listPendingSyncChanges(repository, "family-1");

    expect(event.eventType).toBe("ultrasound");
    expect(event.attachmentIds).toEqual([attachments[0].id]);
    expect(event.payload).toMatchObject({
      bpdMm: 71,
      efwGram: 1320,
      summary: "28+3 周 · EFW 1320 g · BPD 71 mm"
    });
    expect(attachments).toHaveLength(1);
    expect(attachments[0]).toMatchObject({
      kind: "ultrasound_image",
      originalName: "scan.jpg",
      mimeType: "image/jpeg",
      byteSize: image.size,
      ocrStatus: "not-requested"
    });
    expect(savedBlob?.blob).toBeTruthy();
    expect(pendingChanges).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ entityType: "event", entityId: event.id, operation: "upsert" }),
        expect.objectContaining({ entityType: "attachment", entityId: attachments[0].id, operation: "upsert" })
      ])
    );
  });

  it("lists recent active events newest first with a limit", async () => {
    const repository = createLocalRepository("babylog-service-test");
    const older = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "sleep",
      occurredAt: "2026-05-15T05:00:00+08:00"
    });
    const newest = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "temperature",
      occurredAt: "2026-05-15T08:00:00+08:00"
    });
    const otherFamily = createEvent({
      familyId: "family-2",
      childId: "child-2",
      eventType: "feed",
      occurredAt: "2026-05-15T09:00:00+08:00"
    });

    await repository.put("events", older);
    await repository.put("events", newest);
    await repository.put("events", otherFamily);

    await expect(listRecentEvents(repository, "family-1", 1)).resolves.toEqual([newest]);
  });

  it("summarizes day counts and last events by type", async () => {
    const repository = createLocalRepository("babylog-service-test");
    const morningFeed = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "feed",
      occurredAt: "2026-05-15T06:00:00+08:00"
    });
    const laterFeed = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "feed",
      occurredAt: "2026-05-15T11:00:00+08:00"
    });
    const diaper = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "diaper",
      occurredAt: "2026-05-15T10:00:00+08:00"
    });
    const outside = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "feed",
      occurredAt: "2026-05-16T01:00:00+08:00"
    });

    await repository.put("events", morningFeed);
    await repository.put("events", laterFeed);
    await repository.put("events", diaper);
    await repository.put("events", outside);

    const summary = await summarizeEventDay(
      repository,
      "family-1",
      "2026-05-15T00:00:00+08:00",
      "2026-05-15T23:59:59+08:00"
    );

    expect(summary.counts).toMatchObject({
      feed: 2,
      diaper: 1,
      sleep: 0
    });
    expect(summary.lastByType.feed).toEqual(laterFeed);
    expect(summary.lastByType.diaper).toEqual(diaper);
  });
});
