package app.babylog.nativeapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BabyLogRoomJsonDao {
    @Query("SELECT json FROM repository_json_rows WHERE bucket = :bucket ORDER BY updatedAt DESC, id ASC")
    List<String> listJson(String bucket);

    @Query("SELECT json FROM repository_json_rows WHERE bucket = :bucket AND familyId = :familyId AND deletedAt IS NULL ORDER BY updatedAt DESC, id ASC")
    List<String> listActiveJson(String bucket, String familyId);

    @Query("SELECT json FROM repository_json_rows WHERE bucket = :bucket AND familyId = :familyId AND deletedAt IS NULL ORDER BY COALESCE(occurredAt, updatedAt) DESC, id ASC LIMIT :limit OFFSET :offset")
    List<String> listActiveJsonPage(String bucket, String familyId, int limit, int offset);

    @Query("SELECT json FROM repository_json_rows WHERE bucket = :bucket AND familyId = :familyId AND deletedAt IS NOT NULL ORDER BY updatedAt DESC, id ASC")
    List<String> listDeletedJson(String bucket, String familyId);

    @Query("SELECT json FROM repository_json_rows WHERE bucket = :bucket AND id = :id LIMIT 1")
    String findJsonById(String bucket, String id);

    @Query("SELECT COUNT(*) FROM repository_json_rows WHERE bucket = :bucket")
    int countRows(String bucket);

    @Query("SELECT COALESCE(SUM(LENGTH(json)), 0) FROM repository_json_rows")
    long estimateBytes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(BabyLogRoomJsonRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<BabyLogRoomJsonRow> rows);

    @Query("DELETE FROM repository_json_rows WHERE bucket = :bucket AND id = :id")
    void hardDelete(String bucket, String id);

    @Query("DELETE FROM repository_json_rows WHERE bucket = :bucket")
    void clearBucket(String bucket);

    @Query("DELETE FROM repository_json_rows")
    void clearAll();
}
