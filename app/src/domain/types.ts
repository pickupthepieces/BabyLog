export type BackendConfig = {
  enabled: boolean;
  backendBaseUrl: string;
  region: string;
  lastHealthCheck: string | null;
};

export type AdapterHealth =
  | { ok: true; code: "OK" }
  | { ok: false; code: "BACKEND_NOT_CONFIGURED" | "BACKEND_UNREACHABLE" };

export type EntityMetadata = {
  familyId: string;
  childId: string;
  createdAt: string;
  updatedAt: string;
  updatedBy: "local" | string;
  schemaVersion: number;
  deletedAt: string | null;
};

export type FamilyProfile = {
  id: string;
  familyId: string;
  displayName: string;
  createdAt: string;
  updatedAt: string;
  updatedBy: "local" | string;
  schemaVersion: number;
  deletedAt: string | null;
};

export type ChildProfile = EntityMetadata & {
  id: string;
  displayName: string;
  expectedDueDate: string | null;
  birthDate: string | null;
  sex: "unknown" | "female" | "male";
  isCurrentSingleton: true;
};

export type CreateFamilyProfileInput = {
  displayName: string;
};

export type CreateChildProfileInput = {
  familyId: string;
  displayName: string;
  expectedDueDate?: string;
  birthDate?: string;
  sex?: ChildProfile["sex"];
};

export type OcrResult = {
  status: "manual-review-required" | "recognized";
  rawText: string;
  fields: Partial<{
    examDate: string;
    gestationalAgeDays: number;
    bpdMm: number;
    hcMm: number;
    acMm: number;
    flMm: number;
    efwGram: number;
  }>;
};

export const ATTACHMENT_KINDS = [
  "ultrasound_image",
  "vaccine_card",
  "growth_photo",
  "document",
  "other"
] as const;

export type AttachmentKind = (typeof ATTACHMENT_KINDS)[number];

export type AttachmentRecord = EntityMetadata & {
  id: string;
  kind: AttachmentKind;
  originalName: string;
  mimeType: string;
  byteSize: number;
  localBlobKey: string;
  widthPx: number | null;
  heightPx: number | null;
  contentHash: string | null;
  remoteUrl: string | null;
  ocrStatus: "not-requested" | "manual-review-required" | "queued" | "recognized" | "failed";
};

export type SyncEntityType = "event" | "attachment" | "profile" | "settings";
export type SyncOperation = "upsert" | "delete";

export type SyncChange = {
  id: string;
  familyId: string;
  childId: string;
  entityType: SyncEntityType;
  entityId: string;
  operation: SyncOperation;
  status: "pending" | "synced" | "failed";
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
  schemaVersion: number;
};

export type CreateAttachmentInput = {
  familyId: string;
  childId: string;
  kind: AttachmentKind;
  originalName: string;
  mimeType: string;
  byteSize: number;
  localBlobKey: string;
  widthPx?: number;
  heightPx?: number;
  contentHash?: string;
};

export type CreateSyncChangeInput = {
  familyId: string;
  childId: string;
  entityType: SyncEntityType;
  entityId: string;
  operation: SyncOperation;
};

export const EVENT_TYPES = [
  "pregnancy_checkup",
  "ultrasound",
  "fetal_movement",
  "contraction",
  "birth",
  "feed",
  "sleep",
  "diaper",
  "temperature",
  "medication",
  "illness",
  "growth",
  "vaccine",
  "milestone",
  "note"
] as const;

export type EventType = (typeof EVENT_TYPES)[number];

export type BabyLogEvent = EntityMetadata & {
  id: string;
  eventType: EventType;
  occurredAt: string;
  payload: Record<string, unknown>;
  attachmentIds: string[];
  source: "manual" | "ocr" | "import";
};

export type CreateEventInput = {
  familyId: string;
  childId: string;
  eventType: EventType;
  occurredAt: string;
  payload?: Record<string, unknown>;
  attachmentIds?: string[];
  source?: BabyLogEvent["source"];
};

export function createEntityMetadata(familyId: string, childId: string): EntityMetadata {
  const now = new Date().toISOString();

  return {
    familyId,
    childId,
    createdAt: now,
    updatedAt: now,
    updatedBy: "local",
    schemaVersion: 1,
    deletedAt: null
  };
}

export function createFamilyProfile(input: CreateFamilyProfileInput): FamilyProfile {
  const now = new Date().toISOString();
  const id = `fam_${crypto.randomUUID()}`;

  return {
    id,
    familyId: id,
    displayName: input.displayName,
    createdAt: now,
    updatedAt: now,
    updatedBy: "local",
    schemaVersion: 1,
    deletedAt: null
  };
}

export function createChildProfile(input: CreateChildProfileInput): ChildProfile {
  const id = `child_${crypto.randomUUID()}`;

  return {
    id,
    ...createEntityMetadata(input.familyId, id),
    displayName: input.displayName,
    expectedDueDate: input.expectedDueDate ?? null,
    birthDate: input.birthDate ?? null,
    sex: input.sex ?? "unknown",
    isCurrentSingleton: true
  };
}

export function isEventType(value: string): value is EventType {
  return EVENT_TYPES.includes(value as EventType);
}

export function createEvent(input: CreateEventInput): BabyLogEvent {
  return {
    id: `evt_${crypto.randomUUID()}`,
    ...createEntityMetadata(input.familyId, input.childId),
    eventType: input.eventType,
    occurredAt: input.occurredAt,
    payload: input.payload ?? {},
    attachmentIds: input.attachmentIds ?? [],
    source: input.source ?? "manual"
  };
}

export function createAttachmentRecord(input: CreateAttachmentInput): AttachmentRecord {
  return {
    id: `att_${crypto.randomUUID()}`,
    ...createEntityMetadata(input.familyId, input.childId),
    kind: input.kind,
    originalName: input.originalName,
    mimeType: input.mimeType,
    byteSize: input.byteSize,
    localBlobKey: input.localBlobKey,
    widthPx: input.widthPx ?? null,
    heightPx: input.heightPx ?? null,
    contentHash: input.contentHash ?? null,
    remoteUrl: null,
    ocrStatus: "not-requested"
  };
}

export function createSyncChange(input: CreateSyncChangeInput): SyncChange {
  const now = new Date().toISOString();

  return {
    id: `chg_${crypto.randomUUID()}`,
    familyId: input.familyId,
    childId: input.childId,
    entityType: input.entityType,
    entityId: input.entityId,
    operation: input.operation,
    status: "pending",
    attemptCount: 0,
    lastError: null,
    createdAt: now,
    updatedAt: now,
    schemaVersion: 1
  };
}
