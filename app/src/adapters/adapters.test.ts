import { describe, expect, it } from "vitest";
import { createDefaultBackendConfig, createMockSyncAdapter } from "./backend";
import { createManualOnlyOcrAdapter } from "./ocr";
import { createEntityMetadata } from "../domain/types";

describe("adapter boundaries", () => {
  it("defaults backend sync to disabled with an empty base URL", () => {
    const config = createDefaultBackendConfig();

    expect(config.enabled).toBe(false);
    expect(config.backendBaseUrl).toBe("");
    expect(config.region).toBe("");
  });

  it("returns backend-not-configured when health check runs while disabled", async () => {
    const adapter = createMockSyncAdapter(createDefaultBackendConfig());

    await expect(adapter.healthCheck()).resolves.toEqual({
      ok: false,
      code: "BACKEND_NOT_CONFIGURED"
    });
  });

  it("keeps OCR in manual review mode until the backend exists", async () => {
    const adapter = createManualOnlyOcrAdapter();

    await expect(adapter.recognizeUltrasoundImage(new File([""], "scan.jpg"))).resolves.toMatchObject({
      status: "manual-review-required",
      fields: {}
    });
  });

  it("creates sync-ready entity metadata", () => {
    const metadata = createEntityMetadata("family-1", "child-1");

    expect(metadata.familyId).toBe("family-1");
    expect(metadata.childId).toBe("child-1");
    expect(metadata.updatedBy).toBe("local");
    expect(metadata.schemaVersion).toBe(1);
    expect(metadata.deletedAt).toBeNull();
    expect(Date.parse(metadata.updatedAt)).not.toBeNaN();
  });
});
