import { describe, expect, it } from "vitest";
import { loadSyncSettings, saveSyncSettings, syncSettingsStorageKey } from "./syncSettings";

class MemoryStorage {
  private readonly values = new Map<string, string>();

  constructor(initial?: Record<string, string>) {
    Object.entries(initial ?? {}).forEach(([key, value]) => this.values.set(key, value));
  }

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}

describe("sync settings storage", () => {
  it("loads a disabled default when settings are missing", () => {
    expect(loadSyncSettings(new MemoryStorage())).toEqual({
      enabled: false,
      backendBaseUrl: "",
      region: "",
      lastHealthCheck: null
    });
  });

  it("falls back to the disabled default when stored settings are invalid", () => {
    const storage = new MemoryStorage({ [syncSettingsStorageKey]: "{not valid json" });

    expect(loadSyncSettings(storage)).toEqual({
      enabled: false,
      backendBaseUrl: "",
      region: "",
      lastHealthCheck: null
    });
  });

  it("normalizes and persists configured server settings", () => {
    const storage = new MemoryStorage();
    const saved = saveSyncSettings(
      {
        enabled: true,
        backendBaseUrl: " https://example.invalid/api/ ",
        region: " Tokyo ",
        lastHealthCheck: "2026-05-15T00:00:00.000Z"
      },
      storage
    );

    expect(saved).toEqual({
      enabled: true,
      backendBaseUrl: "https://example.invalid/api",
      region: "Tokyo",
      lastHealthCheck: "2026-05-15T00:00:00.000Z"
    });
    expect(loadSyncSettings(storage)).toEqual(saved);
  });

  it("keeps sync disabled when the server URL is empty", () => {
    const saved = saveSyncSettings(
      {
        enabled: true,
        backendBaseUrl: "   ",
        region: "Tokyo",
        lastHealthCheck: null
      },
      new MemoryStorage()
    );

    expect(saved).toEqual({
      enabled: false,
      backendBaseUrl: "",
      region: "Tokyo",
      lastHealthCheck: null
    });
  });
});
