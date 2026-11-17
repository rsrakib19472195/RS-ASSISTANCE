package com.rsassistance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/**
 * User-started, visible foreground wake listener.
 * Uses Android's SpeechRecognizer; it is not an offline keyword model.
 */
public class RsWakeWordService extends Service {
    public static final String ACTION_START = "com.rsassistance.action.START_WAKE";
    public static final String ACTION_STOP = "com.rsassistance.action.STOP_WAKE";
    private static final String CHANNEL_ID = "rs_wake_listener";
    private static final int NOTIFICATION_ID = 19472;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean active = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        active = true;
        getSharedPreferences("rs_wake_state", MODE_PRIVATE).edit()
                .putBoolean("running", true).apply();
        startRecognizer();
        return START_STICKY;
    }

    private void startRecognizer() {
        if (!active) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return;
        }
        try {
            if (recognizer != null) {
                try { recognizer.destroy(); } catch (Exception ignored) {}
            }
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(android.os.Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    scheduleRestart(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1800 : 900);
                }
                @Override public void onResults(android.os.Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty() && isWakePhrase(matches.get(0))) {
                        launchRS(matches.get(0));
                    } else {
                        scheduleRestart(500);
                    }
                }
                @Override public void onPartialResults(android.os.Bundle partialResults) {}
                @Override public void onEvent(int eventType, android.os.Bundle params) {}
            });

            Intent request = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
            request.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizer.startListening(request);
        } catch (Exception e) {
            scheduleRestart(2000);
        }
    }

    private boolean isWakePhrase(String phrase) {
        if (phrase == null) return false;
        String lower = phrase.toLowerCase(Locale.ROOT).trim();
        String compact = lower.replaceAll("[^\\p{L}\\p{N}]", "");
        return compact.contains("আরএস")
                || lower.matches(".*\\brs\\b.*")
                || lower.startsWith("hey rs")
                || lower.startsWith("hello rs");
    }

    private void scheduleRestart(long delayMs) {
        if (!active) return;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::startRecognizer, delayMs);
    }

    private void launchRS(String phrase) {
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) {}
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra(MainActivity.EXTRA_START_LISTENING, true);
        open.putExtra("rs_wake_phrase", phrase);
        startActivity(open);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "RS Background Wake", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible notification while RS listens for its wake phrase.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 19472, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("RS Background Wake is ON")
                .setContentText("Wake phrase শুনলেই RS জেগে উঠবে। বন্ধ করতে RS অ্যাপ খুলুন।")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) {}
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
        getSharedPreferences("rs_wake_state", MODE_PRIVATE).edit()
                .putBoolean("running", false).apply();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
