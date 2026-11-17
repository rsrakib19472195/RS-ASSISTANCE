package com.rsassistance;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends the recognized user message to the protected RS AI Brain Worker. */
public final class RsBrainClient {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(String reply);
        void onError(String message);
    }

    private RsBrainClient() {}

    public static void ask(Activity activity, String message, JSONArray history,
                           Callback callback) {
        if (activity == null || activity.isFinishing()) return;

        if (BuildConfig.RS_APP_TOKEN == null || BuildConfig.RS_APP_TOKEN.isEmpty()) {
            activity.runOnUiThread(() ->
                    callback.onError("App token is not configured."));
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String base = BuildConfig.RS_API_BASE_URL;
                if (!base.endsWith("/")) base += "/";
                URL url = new URL(base + "brain");

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization",
                        "Bearer " + BuildConfig.RS_APP_TOKEN);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");

                JSONObject body = new JSONObject();
                body.put("message", message);
                body.put("history", history == null ? new JSONArray() : history);

                byte[] requestBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(requestBytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBytes);
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                        ? connection.getInputStream() : connection.getErrorStream();

                String responseText;
                try (InputStream input = stream;
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    if (input == null) throw new IllegalStateException("Empty server response");
                    byte[] chunk = new byte[4096];
                    int count;
                    while ((count = input.read(chunk)) != -1) {
                        buffer.write(chunk, 0, count);
                        if (buffer.size() > 64 * 1024) {
                            throw new IllegalStateException("Server response too large");
                        }
                    }
                    responseText = buffer.toString("UTF-8");
                }

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Brain server HTTP " + code);
                }

                JSONObject response = new JSONObject(responseText);
                String reply = response.optString("reply", "").trim();
                if (!response.optBoolean("success", false) || reply.isEmpty()) {
                    throw new IllegalStateException("Brain returned an invalid response");
                }

                String finalReply = reply;
                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing()) callback.onSuccess(finalReply);
                });
            } catch (Exception error) {
                String reason = error.getMessage() == null
                        ? "Connection error" : error.getMessage();
                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing()) callback.onError(reason);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
