export type ImageCandidate = {
  name: string;
  type: string;
  size: number;
  widthPx?: number;
  heightPx?: number;
};

export type AttachmentBlobRecord = {
  id: string;
  familyId: string;
  attachmentId: string;
  blob: Blob;
  mimeType: string;
  byteSize: number;
  createdAt: string;
};

export type CreateAttachmentBlobRecordInput = {
  familyId: string;
  attachmentId: string;
  blob: Blob;
};

export type ImageCompressionPlan =
  | {
      shouldCompress: true;
      reason: "image-too-large" | "dimensions-too-large";
      targetMimeType: "image/jpeg";
      maxDimensionPx: number;
      quality: number;
    }
  | {
      shouldCompress: false;
      reason: "non-image" | "already-small";
    };

export type ImageCompressionPolicy = {
  maxBytesBeforeCompression: number;
  maxDimensionPx: number;
  quality: number;
  targetMimeType: "image/jpeg";
};

export const DEFAULT_IMAGE_COMPRESSION_POLICY: ImageCompressionPolicy = {
  maxBytesBeforeCompression: 800_000,
  maxDimensionPx: 2048,
  quality: 0.82,
  targetMimeType: "image/jpeg"
};

export function createAttachmentBlobRecord(input: CreateAttachmentBlobRecordInput): AttachmentBlobRecord {
  return {
    id: `blob_${input.attachmentId}`,
    familyId: input.familyId,
    attachmentId: input.attachmentId,
    blob: input.blob,
    mimeType: input.blob.type || "application/octet-stream",
    byteSize: input.blob.size,
    createdAt: new Date().toISOString()
  };
}

export function createImageCompressionPlan(
  image: ImageCandidate,
  policy = DEFAULT_IMAGE_COMPRESSION_POLICY
): ImageCompressionPlan {
  if (!image.type.startsWith("image/")) {
    return { shouldCompress: false, reason: "non-image" };
  }

  if (image.size > policy.maxBytesBeforeCompression) {
    return {
      shouldCompress: true,
      reason: "image-too-large",
      targetMimeType: policy.targetMimeType,
      maxDimensionPx: policy.maxDimensionPx,
      quality: policy.quality
    };
  }

  const longestEdge = Math.max(image.widthPx ?? 0, image.heightPx ?? 0);
  if (longestEdge > policy.maxDimensionPx) {
    return {
      shouldCompress: true,
      reason: "dimensions-too-large",
      targetMimeType: policy.targetMimeType,
      maxDimensionPx: policy.maxDimensionPx,
      quality: policy.quality
    };
  }

  return { shouldCompress: false, reason: "already-small" };
}

export function formatStorageEstimate(estimate: StorageEstimate | null): string {
  if (!estimate?.usage || !estimate.quota) {
    return "Storage estimate unavailable";
  }

  const usage = formatBytes(estimate.usage);
  const quota = formatBytes(estimate.quota);
  const percent = Math.round((estimate.usage / estimate.quota) * 100);

  return `${usage} / ${quota} (${percent}%)`;
}

function formatBytes(bytes: number): string {
  const megabytes = bytes / 1024 / 1024;

  return `${megabytes.toFixed(1)} MB`;
}
