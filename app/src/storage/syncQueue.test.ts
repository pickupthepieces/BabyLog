import { beforeEach, describe, expect, it } from "vitest";
import { createSyncChange } from "../domain/types";
import { createLocalRepository } from "./localRepository";
import { listPendingSyncChanges } from "./syncQueue";

describe("sync change queue", () => {
  beforeEach(() => {
    indexedDB.deleteDatabase("babylog-sync-test");
  });

  it("creates pending entity-level changes for future cloud sync", () => {
    const change = createSyncChange({
      familyId: "family-1",
      childId: "child-1",
      entityType: "event",
      entityId: "evt_1",
      operation: "upsert"
    });

    expect(change.id).toMatch(/^chg_/);
    expect(change.familyId).toBe("family-1");
    expect(change.entityType).toBe("event");
    expect(change.entityId).toBe("evt_1");
    expect(change.operation).toBe("upsert");
    expect(change.status).toBe("pending");
    expect(change.attemptCount).toBe(0);
    expect(change.lastError).toBeNull();
  });

  it("lists only pending changes for the active family", async () => {
    const repository = createLocalRepository("babylog-sync-test");
    const pending = createSyncChange({
      familyId: "family-1",
      childId: "child-1",
      entityType: "attachment",
      entityId: "att_1",
      operation: "upsert"
    });
    const otherFamily = createSyncChange({
      familyId: "family-2",
      childId: "child-2",
      entityType: "event",
      entityId: "evt_2",
      operation: "delete"
    });
    const alreadySynced = { ...pending, id: "chg_synced", status: "synced" as const };

    await repository.put("syncChanges", pending);
    await repository.put("syncChanges", otherFamily);
    await repository.put("syncChanges", alreadySynced);

    await expect(listPendingSyncChanges(repository, "family-1")).resolves.toEqual([pending]);
  });
});
