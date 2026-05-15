import type { AttachmentRecord, BabyLogEvent, ChildProfile, FamilyProfile, SyncChange } from "../domain/types";
import type { AttachmentBlobRecord } from "./attachments";
import { base64ToBlob, blobToBase64 } from "./attachments";

export const BACKUP_FORMAT = "babylog.backup";
export const BACKUP_VERSION = 1;

export type BackupAttachmentBlob = {
  familyId: string;
  attachmentId: string;
  mimeType: string;
  byteSize: number;
  createdAt: string;
  dataBase64: string;
};

export type BabyLogBackup = {
  format: typeof BACKUP_FORMAT;
  version: typeof BACKUP_VERSION;
  exportedAt: string;
  data: {
    familyProfiles: FamilyProfile[];
    childProfiles: ChildProfile[];
    events: BabyLogEvent[];
    attachments: AttachmentRecord[];
    attachmentBlobs: BackupAttachmentBlob[];
    syncChanges: SyncChange[];
  };
};

export type BabyLogBackupInput = {
  familyProfiles?: FamilyProfile[];
  childProfiles?: ChildProfile[];
  events: BabyLogEvent[];
  attachments?: AttachmentRecord[];
  attachmentBlobs?: BackupAttachmentBlob[];
  syncChanges?: SyncChange[];
};

export type BabyLogCompleteBackupInput = Omit<BabyLogBackupInput, "attachmentBlobs"> & {
  attachmentBlobs?: AttachmentBlobRecord[];
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
      attachmentBlobs: data.attachmentBlobs ?? [],
      syncChanges: data.syncChanges ?? []
    }
  };
}

export async function createCompleteBackup(data: BabyLogCompleteBackupInput): Promise<BabyLogBackup> {
  const attachmentBlobs = await Promise.all((data.attachmentBlobs ?? []).map(serializeAttachmentBlob));

  return createBackup({
    ...data,
    attachmentBlobs
  });
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

  if (parsed.data.attachmentBlobs !== undefined && !Array.isArray(parsed.data.attachmentBlobs)) {
    throw new Error("Invalid BabyLog backup attachment blobs");
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
      attachmentBlobs: parsed.data.attachmentBlobs ?? [],
      syncChanges: parsed.data.syncChanges ?? []
    }
  };
}

export function restoreAttachmentBlobRecord(record: BackupAttachmentBlob): AttachmentBlobRecord {
  return {
    id: `blob_${record.attachmentId}`,
    familyId: record.familyId,
    attachmentId: record.attachmentId,
    blob: base64ToBlob(record.dataBase64, record.mimeType),
    dataBase64: record.dataBase64,
    mimeType: record.mimeType,
    byteSize: record.byteSize,
    createdAt: record.createdAt
  };
}

async function serializeAttachmentBlob(record: AttachmentBlobRecord): Promise<BackupAttachmentBlob> {
  return {
    familyId: record.familyId,
    attachmentId: record.attachmentId,
    mimeType: record.mimeType,
    byteSize: record.byteSize,
    createdAt: record.createdAt,
    dataBase64: record.dataBase64 ?? (await blobToBase64(record.blob))
  };
}
