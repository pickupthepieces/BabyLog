export type StoragePersistenceState = {
  persisted: boolean;
  supported: boolean;
};

export async function requestStoragePersistence(): Promise<StoragePersistenceState> {
  if (!navigator.storage?.persist) {
    return { persisted: false, supported: false };
  }

  return {
    persisted: await navigator.storage.persist(),
    supported: true
  };
}

export async function estimateStorageUsage(): Promise<StorageEstimate | null> {
  if (!navigator.storage?.estimate) {
    return null;
  }

  return navigator.storage.estimate();
}
