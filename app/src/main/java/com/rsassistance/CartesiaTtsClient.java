package com.rsassistance;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Calls the protected RS Worker for cloned-voice MP3 audio.
 * Android TextToSpeech remains the fallback if the network/provider fails.
 */
public final class CartesiaTtsClient {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CartesiaTtsClient() {}

    public static void speak(Activity activity, String text, String language,
                             TextToSpeech fallback, AssistantFaceView face,
                             TextView status) {
        if (activity == null || activity.isFinishing()) return;

        if (BuildConfig.RS_APP_TOKEN == null || BuildConfig.RS_APP_TOKEN.isEmpty()) {
            useFallback(text, language, fallback, face, status,
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
                connection.setRequestProperty("Accept", "audio/mpeg");

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

                String contentType = connection.getContentType();
                if (contentType == null || !contentType.toLowerCase().contains("audio/mpeg")) {
                    throw new IllegalStateException("Worker response is not audio/mpeg");
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

                // MP3 may begin with an ID3 tag or an MPEG frame; do not require a WAV/RIFF header.
                if (audio.length < 128) {
                    throw new IllegalStateException("MP3 response is empty or incomplete");
                }

                byte[] finalAudio = audio;
                MAIN.post(() -> {
                    if (!activity.isFinishing()) {
                        playAudio(activity, finalAudio, text, language, fallback, face, status);
                    }
                });
            } catch (Exception error) {
                MAIN.post(() -> useFallback(text, language, fallback, face, status,
                        "RS custom voice unavailable; using device voice."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static void playAudio(Activity activity, byte[] audio, String text, String language,
                                  TextToSpeech fallback, AssistantFaceView face,
                                  TextView status) {
        File file = null;
        MediaPlayer player = new MediaPlayer();
        try {
            file = File.createTempFile("rs_voice_", ".mp3", activity.getCacheDir());
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(audio);
            }

            File audioFile = file;
            player.setDataSource(audioFile.getAbsolutePath());
            player.setOnPreparedListener(mp -> {
                try {
                    if (face != null) face.setEmotion(AssistantFaceView.Emotion.SPEAKING);
                    mp.start();
                    if (status != null) status.setText("RS custom voice playing.");
                } catch (Exception playbackError) {
                    try { mp.release(); } catch (Exception ignored) {}
                    audioFile.delete();
                    useFallback(text, language, fallback, face, status,
                            "Audio start failed; using device voice.");
                }
            });
            player.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) {}
                audioFile.delete();
                if (face != null) face.setEmotion(AssistantFaceView.Emotion.IDLE);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                try { mp.release(); } catch (Exception ignored) {}
                audioFile.delete();
                useFallback(text, language, fallback, face, status,
                        "MP3 playback error (" + what + "/" + extra + "); using device voice.");
                return true;
            });
            player.prepareAsync();
        } catch (Exception error) {
            try { player.release(); } catch (Exception ignored) {}
            if (file != null) file.delete();
            useFallback(text, language, fallback, face, status,
                    "Audio setup failed: " + error.getClass().getSimpleName());
        }
    }

    private static void useFallback(String text, String language, TextToSpeech fallback,
                                    AssistantFaceView face, TextView status,
                                    String message) {
        if (status != null) status.setText(message);
        if (fallback != null) {
            try {
                if ("hi".equalsIgnoreCase(language)) {
                    fallback.setLanguage(new java.util.Locale("hi", "IN"));
                } else if ("en".equalsIgnoreCase(language)) {
                    fallback.setLanguage(java.util.Locale.US);
                } else {
                    fallback.setLanguage(new java.util.Locale("bn", "BD"));
                }
            } catch (Exception ignored) {}
            fallback.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rs_reply_fallback");
        }
        if (face != null) face.setEmotion(AssistantFaceView.Emotion.SPEAKING);
    }
}
