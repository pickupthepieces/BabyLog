import { describe, expect, it } from "vitest";
import { createAttachmentRecord, createChildProfile, createEvent, createFamilyProfile, createSyncChange } from "../domain/types";
import { createBackup, parseBackup } from "./backup";

describe("backup format", () => {
  it("exports a versioned backup payload", () => {
    const event = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "ultrasound",
      occurredAt: "2026-05-15T09:00:00+08:00"
    });

    const family = createFamilyProfile({ displayName: "我们家" });
    const child = createChildProfile({
      familyId: family.id,
      displayName: "宝宝",
      expectedDueDate: "2026-10-01"
    });
    const attachment = createAttachmentRecord({
      familyId: family.id,
      childId: child.id,
      kind: "ultrasound_image",
      originalName: "scan.jpg",
      mimeType: "image/jpeg",
      byteSize: 1_000_000,
      localBlobKey: "blob/family-1/scan.jpg"
    });
    const syncChange = createSyncChange({
      familyId: family.id,
      childId: child.id,
      entityType: "event",
      entityId: event.id,
      operation: "upsert"
    });

    const backup = createBackup({
      familyProfiles: [family],
      childProfiles: [child],
      events: [event],
      attachments: [attachment],
      syncChanges: [syncChange]
    });

    expect(backup.format).toBe("babylog.backup");
    expect(backup.version).toBe(1);
    expect(backup.exportedAt).toEqual(expect.any(String));
    expect(backup.data.familyProfiles).toHaveLength(1);
    expect(backup.data.childProfiles).toHaveLength(1);
    expect(backup.data.events).toHaveLength(1);
    expect(backup.data.attachments).toHaveLength(1);
    expect(backup.data.syncChanges).toHaveLength(1);
  });

  it("parses supported backups", () => {
    const backup = createBackup({ events: [] });

    expect(backup.data.familyProfiles).toEqual([]);
    expect(backup.data.childProfiles).toEqual([]);
    expect(backup.data.attachments).toEqual([]);
    expect(backup.data.syncChanges).toEqual([]);
    expect(parseBackup(JSON.stringify(backup))).toEqual(backup);
  });

  it("keeps old event-only backups importable", () => {
    const oldBackup = {
      format: "babylog.backup",
      version: 1,
      exportedAt: "2026-05-15T00:00:00.000Z",
      data: { events: [] }
    };

    expect(parseBackup(JSON.stringify(oldBackup)).data.attachments).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.familyProfiles).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.childProfiles).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.syncChanges).toEqual([]);
  });

  it("rejects unsupported backup versions", () => {
    const backup = createBackup({ events: [] });

    expect(() => parseBackup(JSON.stringify({ ...backup, version: 99 }))).toThrow(/unsupported backup version/i);
  });
});
