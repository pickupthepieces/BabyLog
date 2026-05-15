package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BabyLogStore {
    private static final String PREFS_NAME = "babylog_native_store";
    private static final String RECORDS_KEY = "ultrasound_records";

    private final SharedPreferences preferences;

    public BabyLogStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addRecord(BabyLogRecord record) throws JSONException {
        JSONArray records = loadArray();
        records.put(record.toJson());
        preferences.edit().putString(RECORDS_KEY, records.toString()).apply();
    }

    public List<BabyLogRecord> listRecordsNewestFirst() {
        JSONArray records = loadArray();
        List<BabyLogRecord> result = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            result.add(BabyLogRecord.fromJson(records.optJSONObject(i)));
        }
        Collections.reverse(result);
        return result;
    }

    private JSONArray loadArray() {
        String raw = preferences.getString(RECORDS_KEY, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }
}
