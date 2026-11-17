package com.rsassistance;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small, private, on-device conversation history. */
public final class RsMemoryStore {
    private static final String PREFS = "rs_private_memory";
    private static final String HISTORY = "conversation_history";
    private static final int MAX_TURNS = 24;

    private RsMemoryStore() {}

    public static ArrayList<JSONObject> load(Context context) {
        ArrayList<JSONObject> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray saved = new JSONArray(prefs.getString(HISTORY, "[]"));
            int start = Math.max(0, saved.length() - MAX_TURNS);
            for (int i = start; i < saved.length(); i++) {
                JSONObject item = saved.optJSONObject(i);
                if (item != null) result.add(item);
            }
        } catch (Exception ignored) {
            prefs.edit().remove(HISTORY).apply();
        }
        return result;
    }

    public static void save(Context context, List<JSONObject> turns) {
        JSONArray saved = new JSONArray();
        int start = Math.max(0, turns.size() - MAX_TURNS);
        for (int i = start; i < turns.size(); i++) {
            JSONObject item = turns.get(i);
            if (item != null) saved.put(item);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(HISTORY, saved.toString()).apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(HISTORY).apply();
    }
}
