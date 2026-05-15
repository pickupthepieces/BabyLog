import { beforeEach, describe, expect, it } from "vitest";
import { createAttachmentRecord, createChildProfile, createEvent } from "../domain/types";
import { createLocalRepository } from "./localRepository";
import { createAttachmentBlobRecord } from "./attachments";

describe("local repository", () => {
  beforeEach(() => {
    indexedDB.deleteDatabase("babylog-test");
  });

  it("puts and gets a record by id", async () => {
    const repository = createLocalRepository("babylog-test");
    const event = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "feed",
      occurredAt: "2026-05-15T06:00:00+08:00",
      payload: { amountMl: 90 }
    });

    await repository.put("events", event);

    await expect(repository.get("events", event.id)).resolves.toMatchObject({
      id: event.id,
      eventType: "feed",
      payload: { amountMl: 90 }
    });
  });

  it("lists active records by family and excludes tombstones", async () => {
    const repository = createLocalRepository("babylog-test");
    const event = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "sleep",
      occurredAt: "2026-05-15T07:00:00+08:00"
    });

    await repository.put("events", event);
    await repository.softDelete("events", event.id);

    await expect(repository.listByFamily("events", "family-1")).resolves.toEqual([]);
    await expect(repository.get("events", event.id)).resolves.toMatchObject({
      id: event.id,
      deletedAt: expect.any(String)
    });
  });

  it("queries records by occurred date range", async () => {
    const repository = createLocalRepository("babylog-test");
    const inside = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "temperature",
      occurredAt: "2026-05-15T08:00:00+08:00"
    });
    const outside = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "temperature",
      occurredAt: "2026-05-16T08:00:00+08:00"
    });

    await repository.put("events", inside);
    await repository.put("events", outside);

    await expect(
      repository.queryByDate("events", "family-1", "2026-05-15T00:00:00+08:00", "2026-05-15T23:59:59+08:00")
    ).resolves.toHaveLength(1);
  });

  it("stores attachment metadata separately from event records", async () => {
    const repository = createLocalRepository("babylog-test");
    const attachment = createAttachmentRecord({
      familyId: "family-1",
      childId: "child-1",
      kind: "vaccine_card",
      originalName: "vaccine-card.jpg",
      mimeType: "image/jpeg",
      byteSize: 640_000,
      localBlobKey: "blob/family-1/vaccine-card.jpg"
    });

    await repository.put("attachments", attachment);

    await expect(repository.get("attachments", attachment.id)).resolves.toMatchObject({
      id: attachment.id,
      kind: "vaccine_card",
      localBlobKey: "blob/family-1/vaccine-card.jpg",
      remoteUrl: null
    });
    await expect(repository.listByFamily("attachments", "family-1")).resolves.toHaveLength(1);
  });

  it("stores child profiles for the current family", async () => {
    const repository = createLocalRepository("babylog-test");
    const profile = createChildProfile({
      familyId: "family-1",
      displayName: "宝宝",
      expectedDueDate: "2026-10-01"
    });

    await repository.put("childProfiles", profile);

    await expect(repository.get("childProfiles", profile.id)).resolves.toMatchObject({
      id: profile.id,
      familyId: "family-1",
      displayName: "宝宝",
      isCurrentSingleton: true
    });
  });

  it("stores attachment blobs separately from attachment metadata", async () => {
    const repository = createLocalRepository("babylog-test");
    const blob = new Blob(["binary-scan"], { type: "image/jpeg" });
    const record = createAttachmentBlobRecord({
      familyId: "family-1",
      attachmentId: "att_1",
      blob
    });

    await repository.put("attachmentBlobs", record);

    const saved = await repository.get("attachmentBlobs", record.id);

    expect(saved?.mimeType).toBe("image/jpeg");
    expect(saved?.byteSize).toBe(blob.size);
    expect(saved?.blob).toBeTruthy();
  });
});
