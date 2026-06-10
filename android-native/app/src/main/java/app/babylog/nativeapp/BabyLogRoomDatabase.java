package app.babylog.nativeapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {BabyLogRoomJsonRow.class}, version = 2, exportSchema = false)
public abstract class BabyLogRoomDatabase extends RoomDatabase {
    private static volatile BabyLogRoomDatabase instance;
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE repository_json_rows ADD COLUMN occurredAt TEXT");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_repository_json_rows_bucket_familyId_deletedAt_occurredAt "
                            + "ON repository_json_rows (`bucket`, `familyId`, `deletedAt`, `occurredAt`)"
            );
        }
    };

    public abstract BabyLogRoomJsonDao jsonDao();

    static BabyLogRoomDatabase getInstance(Context context) {
        BabyLogRoomDatabase current = instance;
        if (current != null) {
            return current;
        }
        synchronized (BabyLogRoomDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                                context.getApplicationContext(),
                                BabyLogRoomDatabase.class,
                                "babylog_repository.db"
                        )
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries()
                        .build();
            }
            return instance;
        }
    }
}
