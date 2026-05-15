import { beforeEach, describe, expect, it } from "vitest";
import { createDefaultBackendConfig, createMockSyncAdapter } from "../adapters/backend";
import { createSyncChange } from "../domain/types";
import { createLocalRepository } from "../storage/localRepository";
import { listUnsyncedSyncChanges } from "../storage/syncQueue";
import { runSyncNow } from "./syncService";

describe("sync service", () => {
  beforeEach(() => {
    indexedDB.deleteDatabase("babylog-sync-service-test");
  });

  it("marks retryable changes failed when backend is not configured", async () => {
    const repository = createLocalRepository("babylog-sync-service-test");
    const change = createSyncChange({
      familyId: "family-1",
      childId: "child-1",
      entityType: "event",
      entityId: "evt_1",
      operation: "upsert"
    });

    await repository.put("syncChanges", change);

    const result = await runSyncNow(repository, "family-1", createMockSyncAdapter(createDefaultBackendConfig()));
    const retryable = await listUnsyncedSyncChanges(repository, "family-1");

    expect(result).toEqual({
      ok: false,
      code: "BACKEND_NOT_CONFIGURED",
      attempted: 1
    });
    expect(retryable).toEqual([
      expect.objectContaining({
        id: change.id,
        status: "failed",
        attemptCount: 1,
        lastError: "BACKEND_NOT_CONFIGURED"
      })
    ]);
  });

  it("does nothing when the retryable queue is empty", async () => {
    const repository = createLocalRepository("babylog-sync-service-test");

    await expect(
      runSyncNow(repository, "family-1", createMockSyncAdapter(createDefaultBackendConfig()))
    ).resolves.toEqual({
      ok: true,
      pushed: 0,
      pulled: 0
    });
  });
});
