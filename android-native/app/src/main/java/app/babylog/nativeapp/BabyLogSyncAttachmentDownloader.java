package app.babylog.nativeapp;

import java.util.List;

public final class BabyLogSyncAttachmentDownloader {
    private static final int MAX_DOWNLOADS_PER_RUN = 3;
    private static final long MAX_DOWNLOAD_BYTES_PER_RUN = 10L * 1024L * 1024L;

    private BabyLogSyncAttachmentDownloader() {
    }

    public static DownloadSummary downloadQueuedOnce(
            BabyLogRepository repository,
            BabyLogDomain.BackendConfig backendConfig,
            String familyKey,
            BabyLogRemoteSyncClient remoteClient
    ) {
        if (repository == null) {
            return new DownloadSummary(0, 0, 0, "REPOSITORY_MISSING");
        }
        if (backendConfig == null || !backendConfig.enabled || backendConfig.backendBaseUrl.isEmpty()) {
            return new DownloadSummary(0, 0, 0, "BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return new DownloadSummary(0, 0, 0, "FAMILY_KEY_MISSING");
        }
        BabyLogRemoteSyncClient client = remoteClient == null ? new BabyLogRemoteSyncClient() : remoteClient;
        List<BabyLogRepository.AttachmentDownloadRequest> queue = repository.listAttachmentDownloadQueue();
        int downloaded = 0;
        int failed = 0;
        long bytesDownloaded = 0;
        String lastError = "";
        for (BabyLogRepository.AttachmentDownloadRequest request : queue) {
            if (downloaded >= MAX_DOWNLOADS_PER_RUN || bytesDownloaded >= MAX_DOWNLOAD_BYTES_PER_RUN) {
                break;
            }
            try {
                byte[] sealed = client.downloadAttachmentFile(
                        backendConfig.backendBaseUrl,
                        familyKey,
                        request.remoteRecordId,
                        request.filename
                );
                if (bytesDownloaded + sealed.length > MAX_DOWNLOAD_BYTES_PER_RUN) {
                    break;
                }
                String familyHash = BabyLogFamilyKeyDeriver.lookupHashHex(familyKey);
                byte[] plaintext = BabyLogAttachmentCipher.openFile(
                        BabyLogFamilyKeyDeriver.deriveAttachmentKey(familyKey),
                        BabyLogSyncPushOrchestrator.attachmentAadBytes(request.attachmentId, BabyLogSyncPushOrchestrator.CIPHER_VERSION, familyHash),
                        sealed
                );
                if (repository.putAttachmentBlobFromRemote(request.attachmentId, plaintext, request.fileVersion)) {
                    repository.removeAttachmentDownload(request.attachmentId);
                    downloaded += 1;
                    bytesDownloaded += plaintext.length;
                } else {
                    failed += 1;
                    lastError = "WRITE_FAILED";
                }
            } catch (Exception error) {
                failed += 1;
                lastError = "DOWNLOAD_FAILED";
            }
        }
        return new DownloadSummary(downloaded, failed, bytesDownloaded, lastError);
    }

    public static final class DownloadSummary {
        public final int downloaded;
        public final int failed;
        public final long bytesDownloaded;
        public final String lastError;

        DownloadSummary(int downloaded, int failed, long bytesDownloaded, String lastError) {
            this.downloaded = downloaded;
            this.failed = failed;
            this.bytesDownloaded = bytesDownloaded;
            this.lastError = lastError == null ? "" : lastError;
        }
    }
}
