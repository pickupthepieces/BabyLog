import { describe, expect, it } from "vitest";
import { createAttachmentRecord, createChildProfile, createEvent, createFamilyProfile, createSyncChange } from "../domain/types";
import { createAttachmentBlobRecord } from "./attachments";
import { createBackup, createCompleteBackup, parseBackup } from "./backup";
import { restoreAttachmentBlobRecord } from "./backup";

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
    expect(backup.data.attachmentBlobs).toEqual([]);
    expect(backup.data.syncChanges).toEqual([]);
    expect(parseBackup(JSON.stringify(backup))).toEqual(backup);
  });

  it("exports attachment blobs as base64 in complete backups", async () => {
    const blob = new Blob(["scan-data"], { type: "image/jpeg" });
    const backup = await createCompleteBackup({
      events: [],
      attachmentBlobs: [
        createAttachmentBlobRecord({
          familyId: "family-1",
          attachmentId: "att_1",
          blob
        })
      ]
    });

    expect(backup.data.attachmentBlobs).toEqual([
      expect.objectContaining({
        attachmentId: "att_1",
        familyId: "family-1",
        mimeType: "image/jpeg",
        byteSize: blob.size,
        dataBase64: "c2Nhbi1kYXRh"
      })
    ]);
    expect(parseBackup(JSON.stringify(backup)).data.attachmentBlobs).toHaveLength(1);
  });

  it("restores attachment blob records from backup payloads", async () => {
    const record = restoreAttachmentBlobRecord({
      familyId: "family-1",
      attachmentId: "att_1",
      mimeType: "image/jpeg",
      byteSize: 9,
      createdAt: "2026-05-15T00:00:00.000Z",
      dataBase64: "c2Nhbi1kYXRh"
    });

    expect(record).toMatchObject({
      id: "blob_att_1",
      familyId: "family-1",
      attachmentId: "att_1",
      mimeType: "image/jpeg",
      byteSize: 9,
      dataBase64: "c2Nhbi1kYXRh",
      createdAt: "2026-05-15T00:00:00.000Z"
    });
    await expect(readBlobText(record.blob)).resolves.toBe("scan-data");
  });

  it("keeps old event-only backups importable", () => {
    const oldBackup = {
      format: "babylog.backup",
      version: 1,
      exportedAt: "2026-05-15T00:00:00.000Z",
      data: { events: [] }
    };

    expect(parseBackup(JSON.stringify(oldBackup)).data.attachments).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.attachmentBlobs).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.familyProfiles).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.childProfiles).toEqual([]);
    expect(parseBackup(JSON.stringify(oldBackup)).data.syncChanges).toEqual([]);
  });

  it("rejects unsupported backup versions", () => {
    const backup = createBackup({ events: [] });

    expect(() => parseBackup(JSON.stringify({ ...backup, version: 99 }))).toThrow(/unsupported backup version/i);
  });
});

function readBlobText(blob: Blob): Promise<string> {
  if ("text" in blob && typeof blob.text === "function") {
    return blob.text();
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}
