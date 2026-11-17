package com.rsassistance;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Calls the protected RS Worker for cloned-voice audio.
 * Android TextToSpeech remains the fallback if the network/provider fails.
 */
public final class CartesiaTtsClient {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CartesiaTtsClient() {}

    public static void speak(Activity activity, String text, String language,
                             TextToSpeech fallback, AssistantFaceView face,
                             TextView status) {
        if (BuildConfig.RS_APP_TOKEN == null || BuildConfig.RS_APP_TOKEN.isEmpty()) {
            useFallback(text, fallback, face, status,
                    "Custom voice token is not configured; using device voice.");
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BuildConfig.RS_API_BASE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization",
                        "Bearer " + BuildConfig.RS_APP_TOKEN);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "audio/wav");

                JSONObject body = new JSONObject();
                body.put("transcript", text);
                body.put("language", language);

                byte[] requestBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(requestBytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBytes);
                }

                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Voice backend HTTP " + code);
                }

                byte[] audio;
                try (InputStream input = connection.getInputStream();
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int count;
                    while ((count = input.read(chunk)) != -1) {
                        buffer.write(chunk, 0, count);
                        if (buffer.size() > 8 * 1024 * 1024) {
                            throw new IllegalStateException("Audio response too large");
                        }
                    }
                    audio = buffer.toByteArray();
                }

                if (audio.length < 44
                        || audio[0] != 'R' || audio[1] != 'I'
                        || audio[2] != 'F' || audio[3] != 'F'
                        || audio[8] != 'W' || audio[9] != 'A'
                        || audio[10] != 'V' || audio[11] != 'E') {
                    throw new IllegalStateException("Worker did not return a valid RIFF/WAVE file");
                }

                MAIN.post(() -> playAudio(activity, audio, text, fallback, face, status));
            } catch (Exception error) {
                MAIN.post(() -> useFallback(text, fallback, face, status,
                        "RS custom voice unavailable; using device voice."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static void playAudio(Activity activity, byte[] audio, String text,
                                  TextToSpeech fallback, AssistantFaceView face,
                                  TextView status) {
        java.io.File file = null;
        MediaPlayer player = new MediaPlayer();
        try {
            file = java.io.File.createTempFile("rs_voice_", ".wav", activity.getCacheDir());
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
                output.write(audio);
            }
            java.io.File audioFile = file;
            player.setDataSource(audioFile.getAbsolutePath());
            player.setOnPreparedListener(mp -> {
                try {
                    if (face != null) face.setEmotion(AssistantFaceView.Emotion.SPEAKING);
                    mp.start();
                    if (status != null) status.setText("RS custom voice playing.");
                } catch (Exception playbackError) {
                    mp.release();
                    audioFile.delete();
                    useFallback(text, fallback, face, status,
                            "Audio start failed: " + playbackError.getClass().getSimpleName());
                }
            });
            player.setOnCompletionListener(mp -> {
                mp.release();
                audioFile.delete();
                face.setEmotion(AssistantFaceView.Emotion.IDLE);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                audioFile.delete();
                useFallback(text, fallback, face, status,
                        "Audio decoder error (" + what + "/" + extra + "); using device voice.");
                return true;
            });
            player.prepareAsync();
        } catch (Exception error) {
            player.release();
            if (file != null) file.delete();
            useFallback(text, fallback, face, status,
                    "Audio setup failed: " + error.getClass().getSimpleName()
                            + (error.getMessage() == null ? "" : " — " + error.getMessage()));
        }
    }

    private static void useFallback(String text, TextToSpeech fallback,
                                    AssistantFaceView face, TextView status,
                                    String message) {
        if (status != null) status.setText(message);
        if (fallback != null) {
            fallback.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rs_reply_fallback");
        }
        if (face != null) face.setEmotion(AssistantFaceView.Emotion.SPEAKING);
    }
}
