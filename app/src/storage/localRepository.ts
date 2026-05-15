import type { AttachmentRecord, BabyLogEvent, ChildProfile, FamilyProfile, SyncChange } from "../domain/types";
import type { AttachmentBlobRecord } from "./attachments";

type StoreMap = {
  familyProfiles: FamilyProfile;
  childProfiles: ChildProfile;
  events: BabyLogEvent;
  attachments: AttachmentRecord;
  attachmentBlobs: AttachmentBlobRecord;
  syncChanges: SyncChange;
};
type StoreName = keyof StoreMap;

export type LocalRepository = {
  put: <TStoreName extends StoreName>(storeName: TStoreName, record: StoreMap[TStoreName]) => Promise<void>;
  get: <TStoreName extends StoreName>(storeName: TStoreName, id: string) => Promise<StoreMap[TStoreName] | undefined>;
  listByFamily: <TStoreName extends StoreName>(
    storeName: TStoreName,
    familyId: string
  ) => Promise<StoreMap[TStoreName][]>;
  queryByDate: (
    storeName: "events",
    familyId: string,
    startIso: string,
    endIso: string
  ) => Promise<BabyLogEvent[]>;
  softDelete: <TStoreName extends StoreName>(storeName: TStoreName, id: string) => Promise<void>;
};

const DB_VERSION = 5;

export function createLocalRepository(dbName = "babylog-local"): LocalRepository {
  return {
    async put(storeName, record) {
      const db = await openDatabase(dbName);
      await requestToPromise(transactionStore(db, storeName, "readwrite").put(record));
      db.close();
    },
    async get(storeName, id) {
      const db = await openDatabase(dbName);
      const record = await requestToPromise<StoreMap[typeof storeName] | undefined>(
        transactionStore(db, storeName, "readonly").get(id)
      );
      db.close();
      return record;
    },
    async listByFamily(storeName, familyId) {
      const records = await readAll(dbName, storeName);
      return records.filter((record) => record.familyId === familyId && !isTombstoned(record));
    },
    async queryByDate(storeName, familyId, startIso, endIso) {
      const start = Date.parse(startIso);
      const end = Date.parse(endIso);
      const records = await readAll(dbName, storeName);

      return records.filter((record) => {
        const occurred = Date.parse(record.occurredAt);
        return record.familyId === familyId && record.deletedAt === null && occurred >= start && occurred <= end;
      });
    },
    async softDelete(storeName, id) {
      const db = await openDatabase(dbName);
      const store = transactionStore(db, storeName, "readwrite");
      const record = await requestToPromise<StoreMap[typeof storeName] | undefined>(store.get(id));

      if (record) {
        await requestToPromise(
          store.put({
            ...record,
            updatedAt: new Date().toISOString(),
            deletedAt: new Date().toISOString()
          })
        );
      }

      db.close();
    }
  };
}

async function readAll<TStoreName extends StoreName>(
  dbName: string,
  storeName: TStoreName
): Promise<StoreMap[TStoreName][]> {
  const db = await openDatabase(dbName);
  const records = await requestToPromise<StoreMap[TStoreName][]>(transactionStore(db, storeName, "readonly").getAll());
  db.close();
  return records;
}

function isTombstoned(record: object): boolean {
  return "deletedAt" in record && record.deletedAt !== null;
}

function openDatabase(dbName: string): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(dbName, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains("events")) {
        const store = db.createObjectStore("events", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: false });
        store.createIndex("occurredAt", "occurredAt", { unique: false });
      }

      if (!db.objectStoreNames.contains("familyProfiles")) {
        const store = db.createObjectStore("familyProfiles", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: true });
      }

      if (!db.objectStoreNames.contains("childProfiles")) {
        const store = db.createObjectStore("childProfiles", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: false });
      }

      if (!db.objectStoreNames.contains("attachments")) {
        const store = db.createObjectStore("attachments", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: false });
      }

      if (!db.objectStoreNames.contains("attachmentBlobs")) {
        const store = db.createObjectStore("attachmentBlobs", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: false });
        store.createIndex("attachmentId", "attachmentId", { unique: true });
      }

      if (!db.objectStoreNames.contains("syncChanges")) {
        const store = db.createObjectStore("syncChanges", { keyPath: "id" });
        store.createIndex("familyId", "familyId", { unique: false });
        store.createIndex("status", "status", { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function transactionStore(db: IDBDatabase, storeName: StoreName, mode: IDBTransactionMode): IDBObjectStore {
  return db.transaction(storeName, mode).objectStore(storeName);
}

function requestToPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
