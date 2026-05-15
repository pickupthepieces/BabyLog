package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

public final class BabyLogRecord {
    public final String id;
    public final String examDate;
    public final String gestationalAge;
    public final String bpd;
    public final String hc;
    public final String ac;
    public final String fl;
    public final String efw;
    public final String note;
    public final String photoPath;
    public final long createdAt;

    public BabyLogRecord(
            String id,
            String examDate,
            String gestationalAge,
            String bpd,
            String hc,
            String ac,
            String fl,
            String efw,
            String note,
            String photoPath,
            long createdAt
    ) {
        this.id = id;
        this.examDate = examDate;
        this.gestationalAge = gestationalAge;
        this.bpd = bpd;
        this.hc = hc;
        this.ac = ac;
        this.fl = fl;
        this.efw = efw;
        this.note = note;
        this.photoPath = photoPath;
        this.createdAt = createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("examDate", examDate);
        json.put("gestationalAge", gestationalAge);
        json.put("bpd", bpd);
        json.put("hc", hc);
        json.put("ac", ac);
        json.put("fl", fl);
        json.put("efw", efw);
        json.put("note", note);
        json.put("photoPath", photoPath);
        json.put("createdAt", createdAt);
        return json;
    }

    public static BabyLogRecord fromJson(JSONObject json) {
        if (json == null) {
            return empty();
        }
        return new BabyLogRecord(
                json.optString("id"),
                json.optString("examDate"),
                json.optString("gestationalAge"),
                json.optString("bpd"),
                json.optString("hc"),
                json.optString("ac"),
                json.optString("fl"),
                json.optString("efw"),
                json.optString("note"),
                json.optString("photoPath"),
                json.optLong("createdAt")
        );
    }

    public static BabyLogRecord empty() {
        return new BabyLogRecord("", "", "", "", "", "", "", "", "", "", 0L);
    }
}
