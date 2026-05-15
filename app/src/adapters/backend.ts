import type { AdapterHealth, BackendConfig } from "../domain/types";

export type SyncAdapter = {
  configure: (config: BackendConfig) => void;
  healthCheck: () => Promise<AdapterHealth>;
  pushLocalChanges: () => Promise<{ pushed: number }>;
  pullRemoteChanges: () => Promise<{ pulled: number }>;
};

export function createDefaultBackendConfig(): BackendConfig {
  return {
    enabled: false,
    backendBaseUrl: "",
    region: "",
    lastHealthCheck: null
  };
}

export function createMockSyncAdapter(initialConfig: BackendConfig): SyncAdapter {
  let config = initialConfig;

  return {
    configure(nextConfig) {
      config = nextConfig;
    },
    async healthCheck() {
      if (!config.enabled || config.backendBaseUrl.trim() === "") {
        return { ok: false, code: "BACKEND_NOT_CONFIGURED" };
      }

      return { ok: false, code: "BACKEND_UNREACHABLE" };
    },
    async pushLocalChanges() {
      return { pushed: 0 };
    },
    async pullRemoteChanges() {
      return { pulled: 0 };
    }
  };
}
