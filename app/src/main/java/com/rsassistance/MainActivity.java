package com.rsassistance;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private static final int AUDIO_PERMISSION = 41;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("bn", "BD"));
        });
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
        }
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) { status.setText("শুনছি…"); }
                public void onBeginningOfSpeech() { status.setText("বলুন…"); }
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() { status.setText("ভাবছি…"); }
                public void onError(int e) { status.setText("আবার চেষ্টা করুন"); }
                public void onResults(Bundle b) {
                    ArrayList<String> matches = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) handleSpeech(matches.get(0));
                }
                public void onPartialResults(Bundle b) {}
                public void onEvent(int t, Bundle b) {}
            });
        } else status.setText("Speech Recognition পাওয়া যায়নি");
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(0xFFF1F3F8);

        TextView title = new TextView(this);
        title.setText("RS ASSISTANCE");
        title.setTextSize(28);
        title.setTextColor(0xFF20243A);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("আমি প্রস্তুত। কথা বলতে মাইক্রোফোন চাপুন।");
        status.setTextSize(17);
        status.setTextColor(0xFF50566D);
        status.setGravity(Gravity.CENTER);
        status.setPadding(8, 30, 8, 30);
        root.addView(status);

        Button talk = new Button(this);
        talk.setText("🎙 কথা বলুন");
        talk.setOnClickListener(v -> startListening());
        root.addView(talk);
        setContentView(root);
    }

    private void startListening() {
        if (recognizer == null) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        recognizer.startListening(intent);
    }

    private void handleSpeech(String spoken) {
        String reply = "আমি শুনেছি। এখনো আমার AI brain যুক্ত করা হয়নি।";
        String s = spoken.toLowerCase(Locale.ROOT);
        if (s.contains("হ্যালো") || s.contains("hello")) {
            reply = "হ্যালো! আমি RS ASSISTANCE। কীভাবে সাহায্য করতে পারি?";
        }
        status.setText("আপনি: " + spoken + "\n\nআমি: " + reply);
        if (tts != null) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "rs_reply");
    }

    @Override protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
