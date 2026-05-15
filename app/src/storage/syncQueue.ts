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
