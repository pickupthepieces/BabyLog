import type { SyncChange } from "../domain/types";
import type { LocalRepository } from "./localRepository";

export async function listPendingSyncChanges(
  repository: LocalRepository,
  familyId: string
): Promise<SyncChange[]> {
  const changes = await repository.listByFamily("syncChanges", familyId);

  return changes
    .filter((change) => change.status === "pending")
    .sort((left, right) => Date.parse(left.createdAt) - Date.parse(right.createdAt));
}

export async function listUnsyncedSyncChanges(
  repository: LocalRepository,
  familyId: string
): Promise<SyncChange[]> {
  const changes = await repository.listByFamily("syncChanges", familyId);

  return changes
    .filter((change) => change.status === "pending" || change.status === "failed")
    .sort((left, right) => Date.parse(left.createdAt) - Date.parse(right.createdAt));
}

export async function markSyncChangesFailed(
  repository: LocalRepository,
  changes: SyncChange[],
  errorCode: string
): Promise<void> {
  await Promise.all(
    changes.map((change) =>
      repository.put("syncChanges", {
        ...change,
        status: "failed",
        attemptCount: change.attemptCount + 1,
        lastError: errorCode,
        updatedAt: new Date().toISOString()
      })
    )
  );
}

export async function markSyncChangesSynced(
  repository: LocalRepository,
  changes: SyncChange[]
): Promise<void> {
  await Promise.all(
    changes.map((change) =>
      repository.put("syncChanges", {
        ...change,
        status: "synced",
        lastError: null,
        updatedAt: new Date().toISOString()
      })
    )
  );
}
