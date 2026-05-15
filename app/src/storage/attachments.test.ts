import { describe, expect, it } from "vitest";
import { createAttachmentRecord } from "../domain/types";
import { createAttachmentBlobRecord, createImageCompressionPlan, formatStorageEstimate } from "./attachments";

describe("attachment records", () => {
  it("creates sync-ready attachment metadata without embedding binary data", () => {
    const attachment = createAttachmentRecord({
      familyId: "family-1",
      childId: "child-1",
      kind: "ultrasound_image",
      originalName: "scan.jpg",
      mimeType: "image/jpeg",
      byteSize: 2_500_000,
      localBlobKey: "blob/family-1/scan.jpg",
      widthPx: 3024,
      heightPx: 4032
    });

    expect(attachment.id).toMatch(/^att_/);
    expect(attachment.familyId).toBe("family-1");
    expect(attachment.kind).toBe("ultrasound_image");
    expect(attachment.localBlobKey).toBe("blob/family-1/scan.jpg");
    expect(attachment.remoteUrl).toBeNull();
    expect(attachment.ocrStatus).toBe("not-requested");
    expect(attachment).not.toHaveProperty("binary");
  });

  it("wraps local binary blobs separately from backup metadata", async () => {
    const blob = new Blob(["scan-data"], { type: "image/jpeg" });
    const record = createAttachmentBlobRecord({
      familyId: "family-1",
      attachmentId: "att_1",
      blob
    });

    expect(record.id).toBe("blob_att_1");
    expect(record.familyId).toBe("family-1");
    expect(record.attachmentId).toBe("att_1");
    expect(record.mimeType).toBe("image/jpeg");
    expect(record.byteSize).toBe(blob.size);
    await expect(readBlobText(record.blob)).resolves.toBe("scan-data");
  });
});

describe("image compression planning", () => {
  it("requests compression for large images before local persistence", () => {
    const plan = createImageCompressionPlan({
      name: "ultrasound.png",
      type: "image/png",
      size: 4_000_000,
      widthPx: 3024,
      heightPx: 4032
    });

    expect(plan.shouldCompress).toBe(true);
    expect(plan.reason).toBe("image-too-large");
    expect(plan.targetMimeType).toBe("image/jpeg");
    expect(plan.maxDimensionPx).toBe(2048);
    expect(plan.quality).toBe(0.82);
  });

  it("leaves small images and non-images alone", () => {
    expect(
      createImageCompressionPlan({
        name: "growth.jpg",
        type: "image/jpeg",
        size: 180_000,
        widthPx: 800,
        heightPx: 600
      })
    ).toMatchObject({ shouldCompress: false, reason: "already-small" });

    expect(
      createImageCompressionPlan({
        name: "report.pdf",
        type: "application/pdf",
        size: 1_200_000
      })
    ).toMatchObject({ shouldCompress: false, reason: "non-image" });
  });
});

function readBlobText(blob: Blob): Promise<string> {
  if ("text" in blob && typeof blob.text === "function") {
    return blob.text();
  }

  if ("arrayBuffer" in blob && typeof blob.arrayBuffer === "function") {
    return blob.arrayBuffer().then((buffer) => new TextDecoder().decode(buffer));
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}

describe("storage estimate formatting", () => {
  it("formats browser quota estimates for settings and export reminders", () => {
    expect(formatStorageEstimate({ usage: 5_242_880, quota: 10_485_760 })).toBe("5.0 MB / 10.0 MB (50%)");
  });

  it("handles missing quota estimates", () => {
    expect(formatStorageEstimate(null)).toBe("Storage estimate unavailable");
    expect(formatStorageEstimate({ usage: 0 })).toBe("Storage estimate unavailable");
  });
});
