package de.dipf.edutec.thriller.experiencesampling.util;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesUtil {

    public static List<String> loadListPreference(SharedPreferences sharedPreferences, String key) {
        try {
            JSONArray past_sessions = new JSONArray(sharedPreferences.getString(key, "[]"));
            List<String> pSessions = new ArrayList();
            for (int i = 0; i < past_sessions.length(); i++) {
                pSessions.add((String) past_sessions.get(i));
            }
            return pSessions;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
