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
    private static final int AUDIO_PERMISSION = 41;

    private TextView status;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && tts != null) {
                int languageResult = tts.setLanguage(new Locale("bn", "BD"));
                if (languageResult == TextToSpeech.LANG_MISSING_DATA
                        || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    status.setText("বাংলা TTS এই ডিভাইসে উপলব্ধ নয়।");
                }
            }
        });

        initializeSpeechRecognizer();
        if (recognizer == null) {
            status.setText("এই ডিভাইসে Speech Recognition পাওয়া যায়নি।");
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            status.setText("প্রস্তুত। কথা বলতে মাইক্রোফোন চাপুন এবং অনুমতি দিন।");
        }
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = null;
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("শুনছি…");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("বলুন…");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("ভাবছি…");
            }

            @Override
            public void onError(int error) {
                status.setText(getSpeechErrorMessage(error));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    handleSpeech(matches.get(0));
                } else {
                    status.setText("কথা বুঝতে পারিনি। আবার চেষ্টা করুন।");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private String getSpeechErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "মাইক্রোফোনে সমস্যা হয়েছে। আবার চেষ্টা করুন।";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Speech Recognition বন্ধ হয়েছে। আবার চেষ্টা করুন।";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন।";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "কথা বুঝতে পারিনি। একটু পরিষ্কার করে বলুন।";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "আগের অনুরোধ চলছে। একটু পরে চেষ্টা করুন।";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "কোনো কথা শোনা যায়নি। আবার বলুন।";
            default:
                return "ভয়েস ইনপুটে সমস্যা হয়েছে। আবার চেষ্টা করুন।";
        }
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
        if (recognizer == null) {
            status.setText("এই ডিভাইসে Speech Recognition পাওয়া যায়নি।");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION
            );
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        try {
            recognizer.startListening(intent);
        } catch (IllegalStateException exception) {
            status.setText("ভয়েস সিস্টেম ব্যস্ত। একটু পরে চেষ্টা করুন।");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AUDIO_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText("অনুমতি পাওয়া গেছে। শুনছি…");
                startListening();
            } else {
                status.setText("মাইক্রোফোন অনুমতি ছাড়া ভয়েস ইনপুট ব্যবহার করা যাবে না।");
            }
        }
    }

    private void handleSpeech(String spoken) {
        String reply = "আমি শুনেছি। এখনো আমার AI brain যুক্ত করা হয়নি।";
        String normalized = spoken.toLowerCase(Locale.ROOT);

        if (normalized.contains("হ্যালো") || normalized.contains("hello")) {
            reply = "হ্যালো! আমি RS ASSISTANCE। কীভাবে সাহায্য করতে পারি?";
        }

        status.setText("আপনি: " + spoken + "\n\nআমি: " + reply);

        if (tts != null) {
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "rs_reply");
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        super.onDestroy();
    }
}
