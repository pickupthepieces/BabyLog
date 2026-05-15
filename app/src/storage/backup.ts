import type { AttachmentRecord, BabyLogEvent, ChildProfile, FamilyProfile, SyncChange } from "../domain/types";

export const BACKUP_FORMAT = "babylog.backup";
export const BACKUP_VERSION = 1;

export type BabyLogBackup = {
  format: typeof BACKUP_FORMAT;
  version: typeof BACKUP_VERSION;
  exportedAt: string;
  data: {
    familyProfiles: FamilyProfile[];
    childProfiles: ChildProfile[];
    events: BabyLogEvent[];
    attachments: AttachmentRecord[];
    syncChanges: SyncChange[];
  };
};

export type BabyLogBackupInput = {
  familyProfiles?: FamilyProfile[];
  childProfiles?: ChildProfile[];
  events: BabyLogEvent[];
  attachments?: AttachmentRecord[];
  syncChanges?: SyncChange[];
};

export function createBackup(data: BabyLogBackupInput): BabyLogBackup {
  return {
    format: BACKUP_FORMAT,
    version: BACKUP_VERSION,
    exportedAt: new Date().toISOString(),
    data: {
      familyProfiles: data.familyProfiles ?? [],
      childProfiles: data.childProfiles ?? [],
      events: data.events,
      attachments: data.attachments ?? [],
      syncChanges: data.syncChanges ?? []
    }
  };
}

export function parseBackup(rawJson: string): BabyLogBackup {
  const parsed = JSON.parse(rawJson) as Partial<BabyLogBackup>;

  if (parsed.format !== BACKUP_FORMAT) {
    throw new Error("Invalid BabyLog backup format");
  }

  if (parsed.version !== BACKUP_VERSION) {
    throw new Error(`Unsupported backup version: ${String(parsed.version)}`);
  }

  if (!parsed.data || !Array.isArray(parsed.data.events)) {
    throw new Error("Invalid BabyLog backup data");
  }

  if (parsed.data.attachments !== undefined && !Array.isArray(parsed.data.attachments)) {
    throw new Error("Invalid BabyLog backup attachments");
  }

  if (parsed.data.familyProfiles !== undefined && !Array.isArray(parsed.data.familyProfiles)) {
    throw new Error("Invalid BabyLog backup family profiles");
  }

  if (parsed.data.childProfiles !== undefined && !Array.isArray(parsed.data.childProfiles)) {
    throw new Error("Invalid BabyLog backup child profiles");
  }

  if (parsed.data.syncChanges !== undefined && !Array.isArray(parsed.data.syncChanges)) {
    throw new Error("Invalid BabyLog backup sync changes");
  }

  return {
    ...(parsed as BabyLogBackup),
    data: {
      familyProfiles: parsed.data.familyProfiles ?? [],
      childProfiles: parsed.data.childProfiles ?? [],
      events: parsed.data.events,
      attachments: parsed.data.attachments ?? [],
      syncChanges: parsed.data.syncChanges ?? []
    }
  };
}
