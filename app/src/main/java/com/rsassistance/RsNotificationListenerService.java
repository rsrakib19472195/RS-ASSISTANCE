package com.rsassistance;

import android.app.Notification;
import android.speech.tts.TextToSpeech;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

/** Stores notification previews only after Boss enables Android Notification Access. */
public class RsNotificationListenerService extends NotificationListenerService {
    private TextToSpeech speech;
    private boolean speechReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        speech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && speech != null) {
                int language = speech.setLanguage(new Locale("bn", "BD"));
                speechReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

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

        speakWhatsAppIfEnabled(sbn, title, body);

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

    private void speakWhatsAppIfEnabled(StatusBarNotification sbn, String title, String body) {
        String packageName = sbn.getPackageName();
        boolean whatsapp = "com.whatsapp".equals(packageName)
                || "com.whatsapp.w4b".equals(packageName);
        if (!whatsapp || body.isEmpty() || speech == null || !speechReady) return;

        android.content.SharedPreferences prefs =
                getSharedPreferences("rs_notification_store", MODE_PRIVATE);
        if (!prefs.getBoolean("whatsapp_auto_read", false)) return;

        String fingerprint = sbn.getKey() + "|" + title + "|" + body;
        String previous = prefs.getString("last_spoken_fingerprint", "");
        if (fingerprint.equals(previous)) return;
        prefs.edit().putString("last_spoken_fingerprint", fingerprint).apply();

        String spoken = title.isEmpty() ? body : title + " বলেছে, " + body;
        speech.speak(spoken, TextToSpeech.QUEUE_ADD, null, "rs_whatsapp_notification");
    }

    @Override
    public void onDestroy() {
        if (speech != null) {
            speech.stop();
            speech.shutdown();
            speech = null;
        }
        super.onDestroy();
    }
}
