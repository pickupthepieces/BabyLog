import type { BackendConfig } from "../domain/types";

type SyncSettingsStorage = {
  getItem: (key: string) => string | null;
  setItem: (key: string, value: string) => void;
};

export const syncSettingsStorageKey = "babylog-sync-settings-v1";

export function loadSyncSettings(storage: SyncSettingsStorage | null = getBrowserStorage()): BackendConfig {
  if (!storage) {
    return createDefaultSyncSettings();
  }

  try {
    const raw = storage.getItem(syncSettingsStorageKey);
    if (!raw) {
      return createDefaultSyncSettings();
    }

    return normalizeSyncSettings(JSON.parse(raw));
  } catch {
    return createDefaultSyncSettings();
  }
}

export function saveSyncSettings(
  config: Partial<BackendConfig>,
  storage: SyncSettingsStorage | null = getBrowserStorage()
): BackendConfig {
  const normalized = normalizeSyncSettings(config);

  storage?.setItem(syncSettingsStorageKey, JSON.stringify(normalized));
  return normalized;
}

export function normalizeSyncSettings(config: Partial<BackendConfig>): BackendConfig {
  const backendBaseUrl = typeof config.backendBaseUrl === "string"
    ? config.backendBaseUrl.trim().replace(/\/+$/, "")
    : "";

  return {
    enabled: Boolean(config.enabled && backendBaseUrl),
    backendBaseUrl,
    region: typeof config.region === "string" ? config.region.trim() : "",
    lastHealthCheck: typeof config.lastHealthCheck === "string" ? config.lastHealthCheck : null
  };
}

function createDefaultSyncSettings(): BackendConfig {
  return {
    enabled: false,
    backendBaseUrl: "",
    region: "",
    lastHealthCheck: null
  };
}

function getBrowserStorage(): SyncSettingsStorage | null {
  return typeof window === "undefined" ? null : window.localStorage;
}
