package com.rsassistance;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

/** Stores notification previews only after Boss enables Android Notification Access. */
public class RsNotificationListenerService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null
                || getPackageName().equals(sbn.getPackageName())) return;
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        CharSequence titleValue = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textValue = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (textValue == null || textValue.length() == 0) {
            textValue = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        if ((textValue == null || textValue.length() == 0)
                && notification.extras.containsKey(Notification.EXTRA_MESSAGES)) {
            CharSequence[] lines = notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length > 0) textValue = lines[lines.length - 1];
        }

        String title = titleValue == null ? sbn.getPackageName() : titleValue.toString().trim();
        String body = textValue == null ? "" : textValue.toString().trim();
        if (title.isEmpty() && body.isEmpty()) return;

        try {
            android.content.SharedPreferences prefs =
                    getSharedPreferences("rs_notification_store", MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString("items", "[]"));
            JSONArray next = new JSONArray();
            int start = Math.max(0, old.length() - 39);
            for (int i = start; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null) next.put(item);
            }
            JSONObject item = new JSONObject();
            item.put("package", sbn.getPackageName());
            item.put("title", title);
            item.put("text", body);
            item.put("time", sbn.getPostTime());
            next.put(item);
            prefs.edit().putString("items", next.toString()).apply();
        } catch (Exception ignored) {
            // A malformed old cache should not crash Android's notification pipeline.
        }
    }
}
