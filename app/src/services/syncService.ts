import type { SyncAdapter } from "../adapters/backend";
import type { AdapterHealth } from "../domain/types";
import type { LocalRepository } from "../storage/localRepository";
import { listUnsyncedSyncChanges, markSyncChangesFailed, markSyncChangesSynced } from "../storage/syncQueue";

export type SyncNowResult =
  | { ok: true; pushed: number; pulled: number }
  | { ok: false; code: Extract<AdapterHealth, { ok: false }>["code"]; attempted: number };

export async function runSyncNow(
  repository: LocalRepository,
  familyId: string,
  adapter: SyncAdapter
): Promise<SyncNowResult> {
  const changes = await listUnsyncedSyncChanges(repository, familyId);
  if (changes.length === 0) {
    return { ok: true, pushed: 0, pulled: 0 };
  }

  const health = await adapter.healthCheck();
  if (!health.ok) {
    await markSyncChangesFailed(repository, changes, health.code);
    return { ok: false, code: health.code, attempted: changes.length };
  }

  const pushResult = await adapter.pushLocalChanges();
  const pullResult = await adapter.pullRemoteChanges();
  await markSyncChangesSynced(repository, changes);

  return {
    ok: true,
    pushed: pushResult.pushed,
    pulled: pullResult.pulled
  };
}
