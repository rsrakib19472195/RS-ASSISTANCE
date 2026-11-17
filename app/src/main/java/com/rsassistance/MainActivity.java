package com.rsassistance;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int AUDIO_PERMISSION = 41;

    private TextView status;
    private TextView modeLabel;
    private Button talkButton;
    private Button modeButton;
    private AssistantFaceView face;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean sleeping = false;
    private final ArrayList<JSONObject> conversationHistory = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(5, 17, 16));
        getWindow().setNavigationBarColor(Color.rgb(5, 17, 16));
        buildScreen();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && tts != null) {
                int languageResult = tts.setLanguage(new Locale("bn", "BD"));
                if (languageResult == TextToSpeech.LANG_MISSING_DATA
                        || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    status.setText("এই ডিভাইসে বাংলা TTS পাওয়া যাচ্ছে না।");
                } else {
                    String welcome = "আসসালামু আলাইকুম Boss! আমি RS। আপনাকে স্বাগতম।";
                    status.setText("আসসালামু আলাইকুম, Boss। আমি আপনার জন্য প্রস্তুত।");
                    CartesiaTtsClient.speak(this, welcome, "bn", tts, face, status);
                }
            }
        });
        if (tts != null) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    runOnUiThread(() -> face.setEmotion(AssistantFaceView.Emotion.SPEAKING));
                }
                @Override public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        if (!sleeping) face.setEmotion(AssistantFaceView.Emotion.IDLE);
                    });
                }
                @Override public void onError(String utteranceId) {
                    runOnUiThread(() -> face.setEmotion(AssistantFaceView.Emotion.IDLE));
                }
            });
        }

        initializeSpeechRecognizer();
        if (recognizer == null) {
            status.setText("এই ডিভাইসে Speech Recognition পাওয়া যায়নি।");
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(18));
        root.setBackgroundColor(Color.rgb(5, 17, 16));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        TextView title = new TextView(this);
        title.setText("RS");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(120, 255, 208));
        title.setGravity(Gravity.CENTER);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR PERSONAL AI COMPANION");
        subtitle.setTextSize(10);
        subtitle.setLetterSpacing(0.18f);
        subtitle.setTextColor(Color.rgb(139, 190, 174));
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(68)));

        face = new AssistantFaceView(this);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        faceParams.topMargin = dp(8);
        faceParams.bottomMargin = dp(4);
        root.addView(face, faceParams);

        TextView greeting = new TextView(this);
        greeting.setText("আসসালামু আলাইকুম, Boss");
        greeting.setTextSize(21);
        greeting.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        greeting.setTextColor(Color.WHITE);
        greeting.setGravity(Gravity.CENTER);
        root.addView(greeting);

        status = new TextView(this);
        status.setText("আমি RS। আপনার কথা শোনার জন্য প্রস্তুত।");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(169, 197, 187));
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(8), dp(10), dp(8), dp(14));
        root.addView(status);

        modeLabel = new TextView(this);
        modeLabel.setText("● ACTIVE MODE");
        modeLabel.setTextSize(11);
        modeLabel.setTextColor(Color.rgb(91, 235, 174));
        modeLabel.setGravity(Gravity.CENTER);
        root.addView(modeLabel);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(12), 0, 0);

        talkButton = new Button(this);
        talkButton.setText("🎙  কথা বলুন");
        talkButton.setTextColor(Color.rgb(3, 29, 23));
        talkButton.setTextSize(15);
        talkButton.setAllCaps(false);
        talkButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        talkButton.setBackground(rounded(0xFF73F2C2, dp(18)));
        talkButton.setOnClickListener(v -> startListening());
        LinearLayout.LayoutParams talkParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        talkParams.rightMargin = dp(8);
        controls.addView(talkButton, talkParams);

        modeButton = new Button(this);
        modeButton.setText("ঘুম পাড়াও");
        modeButton.setTextColor(Color.WHITE);
        modeButton.setTextSize(14);
        modeButton.setAllCaps(false);
        modeButton.setBackground(rounded(0xFF183A34, dp(18)));
        modeButton.setOnClickListener(v -> toggleSleepMode());
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        modeParams.leftMargin = dp(8);
        controls.addView(modeButton, modeParams);
        root.addView(controls);

        setContentView(root);
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = null;
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                status.setText("শুনছি, Boss…");
                face.setEmotion(AssistantFaceView.Emotion.LISTENING);
            }
            @Override public void onBeginningOfSpeech() {
                status.setText("বলুন Boss…");
            }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                status.setText("আপনার কথা বুঝতে চেষ্টা করছি…");
                face.setEmotion(AssistantFaceView.Emotion.THINKING);
            }
            @Override public void onError(int error) {
                status.setText(getSpeechErrorMessage(error));
                face.setEmotion(AssistantFaceView.Emotion.IDLE);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handleSpeech(matches.get(0));
                } else {
                    status.setText("কথা বুঝতে পারিনি। আবার চেষ্টা করুন, Boss।");
                    face.setEmotion(AssistantFaceView.Emotion.CONCERNED);
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
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

    private void startListening() {
        if (sleeping) {
            status.setText("আমি Sleep Mode-এ আছি। আগে আমাকে জাগিয়ে দিন, Boss।");
            return;
        }
        if (recognizer == null) {
            status.setText("এই ডিভাইসে Speech Recognition পাওয়া যায়নি।");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        try {
            recognizer.startListening(intent);
        } catch (IllegalStateException exception) {
            status.setText("ভয়েস সিস্টেম ব্যস্ত। একটু পরে চেষ্টা করুন।");
        }
    }

    private void toggleSleepMode() {
        sleeping = !sleeping;
        if (sleeping) {
            if (recognizer != null) recognizer.cancel();
            if (tts != null) tts.stop();
            face.setEmotion(AssistantFaceView.Emotion.SLEEPING);
            modeLabel.setText("● SLEEP MODE — MICROPHONE IDLE");
            modeLabel.setTextColor(Color.rgb(230, 190, 110));
            status.setText("শুভ বিশ্রাম, Boss। জাগানো পর্যন্ত আমি শুনব না।");
            talkButton.setEnabled(false);
            talkButton.setAlpha(0.45f);
            modeButton.setText("জাগাও RS");
        } else {
            face.setEmotion(AssistantFaceView.Emotion.IDLE);
            modeLabel.setText("● ACTIVE MODE");
            modeLabel.setTextColor(Color.rgb(91, 235, 174));
            status.setText("আমি আবার প্রস্তুত, Boss।");
            talkButton.setEnabled(true);
            talkButton.setAlpha(1f);
            modeButton.setText("ঘুম পাড়াও");
        }
    }

    private void handleSpeech(String spoken) {
        status.setText("আপনি: " + spoken + "\n\nRS: ভাবছি, Boss…");
        face.setEmotion(AssistantFaceView.Emotion.THINKING);

        JSONArray history = new JSONArray();
        int start = Math.max(0, conversationHistory.size() - 8);
        for (int i = start; i < conversationHistory.size(); i++) {
            history.put(conversationHistory.get(i));
        }

        try {
            JSONObject userTurn = new JSONObject();
            userTurn.put("role", "user");
            userTurn.put("content", spoken);
            conversationHistory.add(userTurn);
        } catch (Exception ignored) {}

        RsBrainClient.ask(this, spoken, history, new RsBrainClient.Callback() {
            @Override public void onSuccess(String reply) {
                try {
                    JSONObject assistantTurn = new JSONObject();
                    assistantTurn.put("role", "assistant");
                    assistantTurn.put("content", reply);
                    conversationHistory.add(assistantTurn);
                    while (conversationHistory.size() > 16) {
                        conversationHistory.remove(0);
                    }
                } catch (Exception ignored) {}

                status.setText("আপনি: " + spoken + "\n\nRS: " + reply);
                face.setEmotion(AssistantFaceView.Emotion.HAPPY);
                CartesiaTtsClient.speak(
                        MainActivity.this, reply, "bn", tts, face, status
                );
            }

            @Override public void onError(String message) {
                String reply = "দুঃখিত Boss, এই মুহূর্তে আমার Brain-এর সঙ্গে সংযোগ হচ্ছে না। একটু পরে আবার চেষ্টা করুন।";
                status.setText("আপনি: " + spoken + "\n\nRS: " + reply + "\n" + message);
                face.setEmotion(AssistantFaceView.Emotion.CONCERNED);
                CartesiaTtsClient.speak(
                        MainActivity.this, reply, "bn", tts, face, status
                );
            }
        });
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText("অনুমতি পাওয়া গেছে। আবার মাইক্রোফোন চাপুন, Boss।");
            } else {
                status.setText("মাইক্রোফোন অনুমতি ছাড়া ভয়েস ইনপুট ব্যবহার করা যাবে না।");
            }
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
