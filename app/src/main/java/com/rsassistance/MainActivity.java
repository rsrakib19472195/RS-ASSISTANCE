package com.rsassistance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    public static final String EXTRA_START_LISTENING = "rs_start_listening";
    private static final int AUDIO_PERMISSION = 41;
    private static final int CAMERA_PERMISSION = 42;
    private static final int ROLE_REQUEST = 43;
    private static final int CORE_PERMISSIONS = 44;
    private static final int CONTACTS_PERMISSION = 45;

    private TextView status;
    private TextView modeLabel;
    private Button talkButton;
    private Button modeButton;
    private Button setupButton;
    private Button permissionsButton;
    private Button notificationButton;
    private Button inboxButton;
    private Button autoReadButton;
    private Button wakeButton;
    private AssistantFaceView face;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean sleeping = false;
    private boolean autoStartAfterAudioPermission = false;
    private boolean setupAfterAudioPermission = false;
    private String pendingCameraAction;
    private String pendingWriteSettingsAction;
    private boolean pendingAssistantStart = false;
    private boolean pendingStartWakeAfterPermission = false;
    private String pendingWhatsAppCommand;
    private String pendingCallCommand;
    private final ArrayList<JSONObject> conversationHistory = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversationHistory.addAll(RsMemoryStore.load(this));
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
        pendingAssistantStart = getIntent().getBooleanExtra(EXTRA_START_LISTENING, false);
        refreshAssistantSetupLabel();
        requestCorePermissions();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(16), dp(22), dp(14));
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
                LinearLayout.LayoutParams.MATCH_PARENT, dp(62)));

        face = new AssistantFaceView(this);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        faceParams.topMargin = dp(6);
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
        status.setPadding(dp(8), dp(8), dp(8), dp(10));
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
        controls.setPadding(0, dp(10), 0, 0);

        talkButton = new Button(this);
        talkButton.setText("🎙  কথা বলুন");
        talkButton.setTextColor(Color.rgb(3, 29, 23));
        talkButton.setTextSize(15);
        talkButton.setAllCaps(false);
        talkButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        talkButton.setBackground(rounded(0xFF73F2C2, dp(18)));
        talkButton.setOnClickListener(v -> startListening());
        LinearLayout.LayoutParams talkParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        talkParams.rightMargin = dp(8);
        controls.addView(talkButton, talkParams);

        modeButton = new Button(this);
        modeButton.setText("ঘুম পাড়াও");
        modeButton.setTextColor(Color.WHITE);
        modeButton.setTextSize(14);
        modeButton.setAllCaps(false);
        modeButton.setBackground(rounded(0xFF183A34, dp(18)));
        modeButton.setOnClickListener(v -> toggleSleepMode());
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        modeParams.leftMargin = dp(8);
        controls.addView(modeButton, modeParams);
        root.addView(controls);

        LinearLayout setupControls = new LinearLayout(this);
        setupControls.setOrientation(LinearLayout.HORIZONTAL);
        setupControls.setGravity(Gravity.CENTER);
        setupControls.setPadding(0, dp(6), 0, 0);

        setupButton = new Button(this);
        setupButton.setText("Default Assistant সেটআপ");
        setupButton.setTextColor(Color.WHITE);
        setupButton.setTextSize(12);
        setupButton.setAllCaps(false);
        setupButton.setBackground(rounded(0xFF245448, dp(16)));
        setupButton.setOnClickListener(v -> requestAssistantSetup());
        LinearLayout.LayoutParams setupParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        setupParams.rightMargin = dp(6);
        setupControls.addView(setupButton, setupParams);

        Button clearButton = new Button(this);
        clearButton.setText("Memory মুছুন");
        clearButton.setTextColor(Color.WHITE);
        clearButton.setTextSize(12);
        clearButton.setAllCaps(false);
        clearButton.setBackground(rounded(0xFF49312E, dp(16)));
        clearButton.setOnClickListener(v -> confirmClearMemory());
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        clearParams.leftMargin = dp(6);
        setupControls.addView(clearButton, clearParams);
        root.addView(setupControls);

        LinearLayout accessControls = new LinearLayout(this);
        accessControls.setOrientation(LinearLayout.HORIZONTAL);
        accessControls.setGravity(Gravity.CENTER);
        accessControls.setPadding(0, dp(6), 0, 0);

        permissionsButton = new Button(this);
        permissionsButton.setText("সব Permission");
        permissionsButton.setTextColor(Color.WHITE);
        permissionsButton.setTextSize(11);
        permissionsButton.setAllCaps(false);
        permissionsButton.setBackground(rounded(0xFF245448, dp(14)));
        permissionsButton.setOnClickListener(v -> requestCorePermissions());
        LinearLayout.LayoutParams permissionsParams =
                new LinearLayout.LayoutParams(0, dp(44), 1f);
        permissionsParams.rightMargin = dp(4);
        accessControls.addView(permissionsButton, permissionsParams);

        notificationButton = new Button(this);
        notificationButton.setText("Notification Access");
        notificationButton.setTextColor(Color.WHITE);
        notificationButton.setTextSize(10);
        notificationButton.setAllCaps(false);
        notificationButton.setBackground(rounded(0xFF245448, dp(14)));
        notificationButton.setOnClickListener(v -> requestNotificationAccess());
        LinearLayout.LayoutParams notificationParams =
                new LinearLayout.LayoutParams(0, dp(44), 1f);
        notificationParams.leftMargin = dp(4);
        notificationParams.rightMargin = dp(4);
        accessControls.addView(notificationButton, notificationParams);

        inboxButton = new Button(this);
        inboxButton.setText("Inbox");
        inboxButton.setTextColor(Color.WHITE);
        inboxButton.setTextSize(11);
        inboxButton.setAllCaps(false);
        inboxButton.setBackground(rounded(0xFF183A34, dp(14)));
        inboxButton.setOnClickListener(v -> showCapturedNotifications());
        LinearLayout.LayoutParams inboxParams =
                new LinearLayout.LayoutParams(0, dp(44), 0.75f);
        inboxParams.leftMargin = dp(4);
        accessControls.addView(inboxButton, inboxParams);
        root.addView(accessControls);

        autoReadButton = new Button(this);
        autoReadButton.setText("WhatsApp Auto Read: OFF");
        autoReadButton.setTextColor(Color.WHITE);
        autoReadButton.setTextSize(11);
        autoReadButton.setAllCaps(false);
        autoReadButton.setBackground(rounded(0xFF49312E, dp(14)));
        autoReadButton.setOnClickListener(v -> toggleWhatsAppAutoRead());
        LinearLayout.LayoutParams autoReadParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        autoReadParams.topMargin = dp(5);
        root.addView(autoReadButton, autoReadParams);

        wakeButton = new Button(this);
        wakeButton.setText("Background Wake: OFF");
        wakeButton.setTextColor(Color.WHITE);
        wakeButton.setTextSize(11);
        wakeButton.setAllCaps(false);
        wakeButton.setBackground(rounded(0xFF183A34, dp(14)));
        wakeButton.setOnClickListener(v -> toggleBackgroundWake());
        LinearLayout.LayoutParams wakeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        wakeParams.topMargin = dp(5);
        root.addView(wakeButton, wakeParams);

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
            autoStartAfterAudioPermission = true;
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

    private void requestAssistantSetup() {
        requestCorePermissions();
    }

    private void requestCorePermissions() {
        ArrayList<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CONTACTS);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!missing.isEmpty()) {
            status.setText("RS-এর Voice, Flashlight ও notification-এর জন্য প্রয়োজনীয় permission দিন, Boss।");
            requestPermissions(missing.toArray(new String[0]), CORE_PERMISSIONS);
        } else {
            status.setText("প্রাথমিক permission দেওয়া আছে। Default Assistant সেটআপ করছি…");
            requestDefaultAssistantRole();
        }
    }

    private void toggleBackgroundWake() {
        boolean running = getSharedPreferences("rs_wake_state", MODE_PRIVATE)
                .getBoolean("running", false);
        if (running) {
            stopService(new Intent(this, RsWakeWordService.class));
            status.setText("Background Wake বন্ধ করেছি, Boss।");
            refreshAssistantSetupLabel();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
                || (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            pendingStartWakeAfterPermission = true;
            requestCorePermissions();
            return;
        }
        startBackgroundWakeService();
    }

    private void startBackgroundWakeService() {
        try {
            Intent start = new Intent(this, RsWakeWordService.class);
            start.setAction(RsWakeWordService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(start);
            } else {
                startService(start);
            }
            status.setText("RS Background Wake চালু হয়েছে। Wake phrase: RS।");
            refreshAssistantSetupLabel();
        } catch (Exception e) {
            status.setText("Background Wake চালু করা যায়নি। Microphone ও Notification permission পরীক্ষা করুন।");
        }
    }

    private void toggleWhatsAppAutoRead() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("rs_notification_store", MODE_PRIVATE);
        boolean enabled = !prefs.getBoolean("whatsapp_auto_read", false);
        prefs.edit().putBoolean("whatsapp_auto_read", enabled).apply();
        refreshAssistantSetupLabel();
        status.setText(enabled
                ? "WhatsApp notification এলে RS পড়ে শোনাবে, Boss।"
                : "WhatsApp auto-read বন্ধ করেছি, Boss।");
    }

    private void requestNotificationAccess() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            status.setText("Notification Access settings-এ RS-কে Allow করুন, Boss।");
        } catch (Exception e) {
            status.setText("এই ফোনে Notification Access settings খুলতে পারলাম না।");
        }
    }

    private boolean hasNotificationAccess() {
        String enabled = Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(
                new ComponentName(this, RsNotificationListenerService.class).flattenToString());
    }

    private void showCapturedNotifications() {
        String raw = getSharedPreferences("rs_notification_store", MODE_PRIVATE)
                .getString("items", "[]");
        try {
            JSONArray items = new JSONArray(raw);
            StringBuilder message = new StringBuilder();
            int start = Math.max(0, items.length() - 12);
            for (int i = items.length() - 1; i >= start; i--) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String title = item.optString("title", "Notification");
                String body = item.optString("text", "");
                message.append(title).append("\n").append(body).append("\n\n");
            }
            if (message.length() == 0) {
                message.append(hasNotificationAccess()
                        ? "এখনো কোনো নতুন notification পাওয়া যায়নি।"
                        : "প্রথমে Notification Access চালু করুন।");
            }
            new AlertDialog.Builder(this)
                    .setTitle("RS Notification Inbox")
                    .setMessage(message.toString())
                    .setPositiveButton("ঠিক আছে", null)
                    .show();
        } catch (Exception e) {
            status.setText("Notification inbox পড়তে সমস্যা হয়েছে।");
        }
    }

    private void requestDefaultAssistantRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                status.setText("এই Android সংস্করণে Default Assistant role পাওয়া যাচ্ছে না।");
                return;
            }
            if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                status.setText("RS ইতিমধ্যেই আপনার Default Assistant, Boss।");
                refreshAssistantSetupLabel();
                if (pendingAssistantStart
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    pendingAssistantStart = false;
                    startListening();
                }
                return;
            }
            startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                    ROLE_REQUEST);
        } else {
            try {
                startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
            } catch (Exception e) {
                status.setText("এই ফোনে Voice Assistant settings খুলতে পারলাম না।");
            }
        }
    }

    private void refreshAssistantSetupLabel() {
        if (setupButton == null) return;
        if (notificationButton != null) {
            notificationButton.setText(hasNotificationAccess()
                    ? "Notification ✓" : "Notification Access");
        }
        if (autoReadButton != null) {
            boolean enabled = getSharedPreferences("rs_notification_store", MODE_PRIVATE)
                    .getBoolean("whatsapp_auto_read", false);
            autoReadButton.setText("WhatsApp Auto Read: " + (enabled ? "ON" : "OFF"));
            autoReadButton.setBackground(rounded(
                    enabled ? 0xFF176A4D : 0xFF49312E, dp(14)));
        }
        if (wakeButton != null) {
            boolean running = getSharedPreferences("rs_wake_state", MODE_PRIVATE)
                    .getBoolean("running", false);
            wakeButton.setText("Background Wake: " + (running ? "ON" : "OFF"));
            wakeButton.setBackground(rounded(
                    running ? 0xFF176A4D : 0xFF183A34, dp(14)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                setupButton.setText("RS Default Assistant ✓");
                return;
            }
        }
        setupButton.setText("Default Assistant সেটআপ");
    }

    private void confirmClearMemory() {
        new AlertDialog.Builder(this)
                .setTitle("RS Memory মুছবেন?")
                .setMessage("এই ফোনে সংরক্ষিত কথোপকথনের ইতিহাস মুছে যাবে।")
                .setNegativeButton("না", null)
                .setPositiveButton("মুছে ফেলুন", (dialog, which) -> {
                    conversationHistory.clear();
                    RsMemoryStore.clear(this);
                    status.setText("আপনার local conversation memory মুছে দিয়েছি, Boss।");
                })
                .show();
    }

    private void toggleSleepMode() {
        sleeping = !sleeping;
        if (sleeping) {
            try { stopService(new Intent(this, RsWakeWordService.class)); }
            catch (Exception ignored) {}
            if (recognizer != null) recognizer.cancel();
            if (tts != null) tts.stop();
            face.setEmotion(AssistantFaceView.Emotion.SLEEPING);
            modeLabel.setText("● SLEEP MODE — MICROPHONE IDLE");
            modeLabel.setTextColor(Color.rgb(230, 190, 110));
            status.setText("শুভ বিশ্রাম, Boss। জাগানো পর্যন্ত আমি শুনব না।");
            talkButton.setEnabled(false);
            talkButton.setAlpha(0.45f);
            modeButton.setText("জাগাও RS");
            refreshAssistantSetupLabel();
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
        status.setText("আপনি: " + spoken + "\n\nRS: বুঝছি, Boss…");
        face.setEmotion(AssistantFaceView.Emotion.THINKING);

        if (handleOwnerQuestion(spoken)) return;
        if (handleCallCommand(spoken)) return;
        if (handleWhatsAppMessageCommand(spoken)) return;
        if (handleNotificationQuestion(spoken)) return;

        RsActionController.Result action = RsActionController.execute(this, spoken);
        if (action.needsWriteSettings) {
            pendingWriteSettingsAction = spoken;
            try {
                Intent settings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(settings);
                status.setText("Brightness নিয়ন্ত্রণের জন্য RS-কে Modify System Settings দিন, Boss।");
            } catch (Exception e) {
                status.setText("Modify System Settings permission page খুলতে পারলাম না।");
            }
            return;
        }
        if (action.needsCameraPermission) {
            pendingCameraAction = spoken;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            status.setText("ফ্ল্যাশলাইট ব্যবহারের জন্য Camera permission প্রয়োজন, Boss।");
            return;
        }
        if (action.handled) {
            addTurn("user", spoken);
            addTurn("assistant", action.message);
            RsMemoryStore.save(this, conversationHistory);
            status.setText("আপনি: " + spoken + "\n\nRS: " + action.message);
            face.setEmotion(AssistantFaceView.Emotion.HAPPY);
            CartesiaTtsClient.speak(this, action.message, "bn", tts, face, status);
            return;
        }

        JSONArray history = new JSONArray();
        int start = Math.max(0, conversationHistory.size() - 8);
        for (int i = start; i < conversationHistory.size(); i++) {
            history.put(conversationHistory.get(i));
        }

        addTurn("user", spoken);
        RsMemoryStore.save(this, conversationHistory);

        RsBrainClient.ask(this, spoken, history, new RsBrainClient.Callback() {
            @Override public void onSuccess(String reply) {
                addTurn("assistant", reply);
                RsMemoryStore.save(MainActivity.this, conversationHistory);
                status.setText("আপনি: " + spoken + "\n\nRS: " + reply);
                face.setEmotion(AssistantFaceView.Emotion.HAPPY);
                CartesiaTtsClient.speak(
                        MainActivity.this, reply, detectSpeechLanguage(reply), tts, face, status
                );
            }

            @Override public void onError(String message) {
                String reply = "দুঃখিত Boss, এই মুহূর্তে আমার Brain-এর সঙ্গে সংযোগ হচ্ছে না। একটু পরে আবার চেষ্টা করুন।";
                addTurn("assistant", reply);
                RsMemoryStore.save(MainActivity.this, conversationHistory);
                status.setText("আপনি: " + spoken + "\n\nRS: " + reply + "\n" + message);
                face.setEmotion(AssistantFaceView.Emotion.CONCERNED);
                CartesiaTtsClient.speak(
                        MainActivity.this, reply, "bn", tts, face, status
                );
            }
        });
    }

    private String detectSpeechLanguage(String text) {
        if (text == null) return "bn";
        if (text.matches("(?s).*[\\u0900-\\u097F].*")) return "hi";
        if (text.matches("(?s).*[\\u0980-\\u09FF].*")) return "bn";
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches("(?s).*(\\b(ami|tumi|apni|kemon|achi|ase|ache|korbo|koro|ki|keno|bhalo|na|hobe|dao|daw)\\b).*")) {
            return "bn";
        }
        return "en";
    }

    private static final class WhatsAppDraft {
        final String contact;
        final String message;
        WhatsAppDraft(String contact, String message) {
            this.contact = contact;
            this.message = message;
        }
    }

    private boolean handleWhatsAppMessageCommand(String spoken) {
        WhatsAppDraft draft = parseWhatsAppDraft(spoken);
        if (draft == null) return false;
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingWhatsAppCommand = spoken;
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION);
            status.setText("WhatsApp message-এর recipient খুঁজতে Contacts permission দিন, Boss।");
            return true;
        }
        findWhatsAppRecipient(draft);
        return true;
    }

    private WhatsAppDraft parseWhatsAppDraft(String spoken) {
        if (spoken == null) return null;
        String q = spoken.trim();
        Matcher english = Pattern.compile(
                "(?i)^(?:send|whatsapp|message)\\s+(?:a\\s+)?(?:whatsapp\\s+)?(?:message\\s+)?to\\s+(.+?)\\s+(?:saying|message|text)\\s+(.+)$"
        ).matcher(q);
        if (english.find()) return new WhatsAppDraft(english.group(1).trim(), english.group(2).trim());

        Matcher banglish = Pattern.compile(
                "(?i)^whatsapp(?:\\s+e)?\\s+(.+?)\\s+ke\\s+(.+?)\\s+(?:pathao|send koro|bhejo)$"
        ).matcher(q);
        if (banglish.find()) return new WhatsAppDraft(banglish.group(1).trim(), banglish.group(2).trim());

        Matcher banglishNatural = Pattern.compile(
                "(?i)^(.+?)\\s+ke\\s+(?:whatsapp(?:\\s+e)?\\s+)?(?:msg|message)\\s+(?:pathao|dao|send koro)\\s*[:,-]?\\s*(.+)$"
        ).matcher(q);
        if (banglishNatural.find()) return new WhatsAppDraft(
                banglishNatural.group(1).trim(), banglishNatural.group(2).trim());

        Matcher bengali = Pattern.compile(
                "হোয়াটসঅ্যাপে\\s*(.+?)\\s*কে\\s*(.+?)\\s*(?:পাঠাও|পাঠিয়ে দাও|পাঠিয়ে দাও)$"
        ).matcher(q);
        if (bengali.find()) return new WhatsAppDraft(bengali.group(1).trim(), bengali.group(2).trim());
        return null;
    }

    private void findWhatsAppRecipient(WhatsAppDraft draft) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        Cursor cursor = null;
        try {
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] args = new String[]{"%" + draft.contact + "%"};
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    selection, args,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );
            if (cursor != null) {
                while (cursor.moveToNext() && names.size() < 20) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    if (name != null && number != null && !number.trim().isEmpty()) {
                        if (!numbers.contains(number)) {
                            names.add(name);
                            numbers.add(number);
                        }
                    }
                }
            }
        } catch (Exception e) {
            status.setText("Contacts পড়তে সমস্যা হয়েছে, Boss।");
            return;
        } finally {
            if (cursor != null) cursor.close();
        }

        if (names.isEmpty()) {
            status.setText("‘" + draft.contact + "’ নামে কোনো contact খুঁজে পেলাম না, Boss।");
            return;
        }
        if (names.size() == 1) {
            confirmWhatsAppDraft(names.get(0), numbers.get(0), draft.message);
            return;
        }
        String[] choices = names.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("কোন contact-এ পাঠাব?")
                .setItems(choices, (dialog, which) ->
                        confirmWhatsAppDraft(names.get(which), numbers.get(which), draft.message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmWhatsAppDraft(String name, String rawNumber, String message) {
        String digits = rawNumber.replaceAll("[^0-9+]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.startsWith("0")) digits = "88" + digits.substring(1);
        final String phone = digits.replace("+", "");
        if (phone.length() < 8) {
            status.setText("এই contact-এর phone number ঠিক নেই, Boss।");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("WhatsApp message পাঠাব?")
                .setMessage("To: " + name + "\n\nMessage: " + message
                        + "\n\nRS WhatsApp chat খুলবে। Send চাপার আগে আপনি message যাচাই করতে পারবেন।")
                .setNegativeButton("না", null)
                .setPositiveButton("WhatsApp খুলুন", (dialog, which) -> {
                    try {
                        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(message));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setPackage("com.whatsapp");
                        if (intent.resolveActivity(getPackageManager()) == null) {
                            intent.setPackage(null);
                        }
                        startActivity(intent);
                        status.setText(name + "-এর WhatsApp chat খুলে দিয়েছি, Boss। Send আপনাকেই চাপতে হবে।");
                    } catch (Exception e) {
                        status.setText("WhatsApp chat খুলতে পারলাম না, Boss।");
                    }
                })
                .show();
    }


    private boolean handleOwnerQuestion(String spoken) {
        String q = spoken == null ? "" : spoken.toLowerCase(Locale.ROOT);
        if (containsAnyText(q, "who made you", "who created you", "who built you",
                "who is your creator", "who is your developer", "developer ke",
                "ke banaise", "ke banayse", "ke banayche", "ke toiri korse",
                "ke toiri koreche", "ke toiri korse", "toke ke banayse",
                "তোমাকে কে বানিয়েছে", "তোমাকে কে বানিয়েছে", "তোমাকে কে তৈরি করেছে",
                "কে বানিয়েছে তোমাকে", "কে বানিয়েছে তোমাকে", "কে তৈরি করেছে")) {
            respondLocally(spoken,
                    "আমাকে তৈরি করেছে আমার Boss, RS RAKIB। তিনিই RS ASSISTANCE-এর owner এবং developer।");
            return true;
        }
        if (containsAnyText(q, "about me", "what do you know about me", "who am i",
                "amar somporke bolo", "amar bepare bolo", "amar porichoy dao",
                "ami ke", "amake cheno", "আমার সম্পর্কে বল", "আমার ব্যাপারে বল",
                "আমাকে চেনো", "আমি কে", "আমার পরিচয় দাও", "আমার পরিচয় দাও")) {
            respondLocally(spoken,
                    "Boss, আপনি RS RAKIB। আপনি RS ASSISTANCE-এর creator ও developer। "
                    + "আপনি বাংলাদেশে থাকেন এবং Bangla ও Banglish-এ কথা বলেন। "
                    + "আপনি Android app, AI assistant, web development—HTML, CSS, JavaScript—"
                    + "এবং Free Fire esports ও নিজের digital projects নিয়ে কাজ করেন। "
                    + "আপনার লক্ষ্য হলো RS-কে ধীরে ধীরে একটি দক্ষ personal AI assistant এবং ভবিষ্যতে robot-এ রূপ দেওয়া। "
                    + "আপনার private information বা কোনো secret আমি অন্যের কাছে প্রকাশ করব না।");
            return true;
        }
        if (containsAnyText(q, "amar nam ki", "what is my name", "আমার নাম কি",
                "আমার নাম কী", "আমাকে কি নামে ডাকবে")) {
            respondLocally(spoken, "আপনার নাম RS RAKIB, Boss। আমি আপনাকে Boss বলেই সম্বোধন করি।");
            return true;
        }
        return false;
    }

    private void respondLocally(String spoken, String reply) {
        addTurn("user", spoken);
        addTurn("assistant", reply);
        RsMemoryStore.save(this, conversationHistory);
        status.setText("আপনি: " + spoken + "\n\nRS: " + reply);
        face.setEmotion(AssistantFaceView.Emotion.HAPPY);
        CartesiaTtsClient.speak(this, reply, detectSpeechLanguage(reply), tts, face, status);
    }

    private boolean handleNotificationQuestion(String spoken) {
        String q = spoken == null ? "" : spoken.toLowerCase(Locale.ROOT);
        boolean asksAboutMessages = containsAnyText(q,
                "last message", "last msg", "recent message", "recent msg",
                "who messaged", "what message", "what did", "message dise",
                "msg dise", "ke msg", "ke message", "ki message", "ki msg",
                "msg ashche", "message ashche", "message asche", "msg asche",
                "ke pathaise", "ke pathiyeche", "ki likheche", "ki likhse",
                "whatsapp inbox", "show inbox", "read inbox",
                "শেষ মেসেজ", "সর্বশেষ মেসেজ", "কে মেসেজ", "কি মেসেজ",
                "কী মেসেজ", "মেসেজ এসেছে", "মেসেজ আসছে", "কে পাঠিয়েছে",
                "কে পাঠিয়েছে", "কি লিখেছে", "কী লিখেছে", "কি বলেছে",
                "কী বলেছে", "ইনবক্স দেখাও", "ইনবক্স পড়", "ইনবক্স পড়");
        if (!asksAboutMessages) return false;

        if (!hasNotificationAccess()) {
            respondLocally(spoken,
                    "Boss, incoming message কে পাঠিয়েছে বা কী লিখেছে তা জানার জন্য "
                    + "প্রথমে Notification Access চালু করতে হবে। RS-এর Notification Access button চাপুন, "
                    + "Settings-এ RS-কে Allow করুন।");
            return true;
        }

        boolean whatsappOnly = containsAnyText(q, "whatsapp", "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ");
        String raw = getSharedPreferences("rs_notification_store", MODE_PRIVATE)
                .getString("items", "[]");
        StringBuilder reply = new StringBuilder();
        try {
            JSONArray items = new JSONArray(raw);
            int shown = 0;
            for (int i = items.length() - 1; i >= 0 && shown < 6; i--) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String packageName = item.optString("package", "");
                boolean whatsapp = "com.whatsapp".equals(packageName)
                        || "com.whatsapp.w4b".equals(packageName);
                if (whatsappOnly && !whatsapp) continue;
                String title = item.optString("title", "").trim();
                String body = item.optString("text", "").trim();
                if (title.isEmpty() && body.isEmpty()) continue;
                if (shown > 0) reply.append("\n\n");
                if (!title.isEmpty()) reply.append(title);
                if (!body.isEmpty()) {
                    if (!title.isEmpty()) reply.append(" বলেছে: ");
                    reply.append(body);
                }
                shown++;
            }
            if (shown == 0) {
                reply.append(whatsappOnly
                        ? "Boss, এখনো কোনো WhatsApp message notification সংরক্ষিত নেই। "
                          + "নতুন message এলে এবং Notification Access চালু থাকলে RS জানতে পারবে।"
                        : "Boss, এখনো পড়ার মতো কোনো message notification পাওয়া যায়নি। "
                          + "Notification Access চালু থাকার পর আসা notification-গুলোই RS দেখতে পারে।");
            } else {
                reply.insert(0, whatsappOnly
                        ? "সর্বশেষ পাওয়া WhatsApp message-গুলো:\n"
                        : "সর্বশেষ পাওয়া message notification-গুলো:\n");
            }
        } catch (Exception e) {
            reply.append("Boss, notification history পড়তে সমস্যা হয়েছে।");
        }
        respondLocally(spoken, reply.toString());
        return true;
    }

    private static final class CallRequest {
        final String contact;
        CallRequest(String contact) { this.contact = contact; }
    }

    private boolean handleCallCommand(String spoken) {
        CallRequest request = parseCallRequest(spoken);
        if (request == null) return false;
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingCallCommand = spoken;
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION);
            status.setText("কাকে call করবেন তা খুঁজতে Contacts permission দিন, Boss।");
            return true;
        }
        findCallRecipient(request);
        return true;
    }

    private CallRequest parseCallRequest(String spoken) {
        if (spoken == null) return null;
        String q = spoken.trim();
        Matcher english = Pattern.compile(
                "(?i)^(?:please\\\\s+)?(?:call|phone|dial)\\\\s+(.+?)(?:\\\\s+now)?$"
        ).matcher(q);
        if (english.find()) return new CallRequest(english.group(1).trim());

        Matcher banglish = Pattern.compile(
                "(?i)^(.+?)\\\\s+ke\\\\s+(?:call|phone)\\\\s+(?:dao|daw|koro|korun)$"
        ).matcher(q);
        if (banglish.find()) return new CallRequest(banglish.group(1).trim());

        Matcher bengali = Pattern.compile(
                "(.+?)\\\\s*কে\\\\s*(?:কল|ফোন)\\\\s*(?:দাও|করো|করুন)$"
        ).matcher(q);
        if (bengali.find()) return new CallRequest(bengali.group(1).trim());
        return null;
    }

    private void findCallRecipient(CallRequest request) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        Cursor cursor = null;
        try {
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER},
                    selection, new String[]{"%" + request.contact + "%"},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (cursor != null) {
                while (cursor.moveToNext() && names.size() < 20) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    if (name != null && number != null && !number.trim().isEmpty()
                            && !numbers.contains(number)) {
                        names.add(name);
                        numbers.add(number);
                    }
                }
            }
        } catch (Exception e) {
            respondLocally("call " + request.contact,
                    "Boss, Contacts থেকে phone number পড়তে পারিনি। Permission পরীক্ষা করুন।");
            return;
        } finally {
            if (cursor != null) cursor.close();
        }
        if (names.isEmpty()) {
            respondLocally("call " + request.contact,
                    "Boss, '" + request.contact + "' নামে কোনো phone contact খুঁজে পেলাম না।");
        } else if (names.size() == 1) {
            confirmCall(names.get(0), numbers.get(0));
        } else {
            String[] choices = names.toArray(new String[0]);
            new AlertDialog.Builder(this).setTitle("কাকে call করবেন?")
                    .setItems(choices, (dialog, which) ->
                            confirmCall(names.get(which), numbers.get(which)))
                    .setNegativeButton("Cancel", null).show();
        }
    }

    private void confirmCall(String name, String number) {
        new AlertDialog.Builder(this)
                .setTitle(name + "-কে call করবেন?")
                .setMessage("RS ফোনের dialer খুলবে। নম্বরটি যাচাই করে Call চাপুন।\n" + number)
                .setNegativeButton("না", null)
                .setPositiveButton("Dial", (dialog, which) -> {
                    try {
                        Intent dial = new Intent(Intent.ACTION_DIAL,
                                Uri.fromParts("tel", number, null));
                        startActivity(dial);
                        status.setText(name + "-এর নম্বর dialer-এ খুলেছি, Boss।");
                    } catch (Exception e) {
                        status.setText("Dialer খুলতে পারলাম না, Boss।");
                    }
                }).show();
    }

    private static boolean containsAnyText(String value, String... parts) {
        for (String part : parts) {
            if (value.contains(part)) return true;
        }
        return false;
    }

    private void addTurn(String role, String content) {
        try {
            JSONObject turn = new JSONObject();
            turn.put("role", role);
            turn.put("content", content);
            conversationHistory.add(turn);
            while (conversationHistory.size() > 24) conversationHistory.remove(0);
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == CORE_PERMISSIONS) {
            refreshAssistantSetupLabel();
            if (pendingStartWakeAfterPermission
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                    && (Build.VERSION.SDK_INT < 33
                    || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED)) {
                pendingStartWakeAfterPermission = false;
                startBackgroundWakeService();
            }
            if (pendingAssistantStart
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                pendingAssistantStart = false;
                startListening();
            }
            requestDefaultAssistantRole();
        } else if (requestCode == AUDIO_PERMISSION) {
            if (granted) {
                status.setText("Microphone permission পাওয়া গেছে, Boss।");
                if (setupAfterAudioPermission) {
                    setupAfterAudioPermission = false;
                    requestDefaultAssistantRole();
                } else if (autoStartAfterAudioPermission) {
                    autoStartAfterAudioPermission = false;
                    startListening();
                }
            } else {
                setupAfterAudioPermission = false;
                autoStartAfterAudioPermission = false;
                status.setText("Microphone permission ছাড়া voice assistant কাজ করবে না।");
            }
        } else if (requestCode == CONTACTS_PERMISSION) {
            String pendingCall = pendingCallCommand;
            pendingCallCommand = null;
            String pending = pendingWhatsAppCommand;
            pendingWhatsAppCommand = null;
            if (granted && pendingCall != null) {
                handleCallCommand(pendingCall);
            } else if (granted && pending != null) {
                handleWhatsAppMessageCommand(pending);
            } else {
                status.setText("Contact permission দেওয়া হয়নি, তাই contact খুঁজতে পারছি না।");
            }
        } else if (requestCode == CAMERA_PERMISSION) {
            String action = pendingCameraAction;
            pendingCameraAction = null;
            if (granted && action != null) {
                handleSpeech(action);
            } else {
                status.setText("Camera permission দেওয়া হয়নি, তাই flashlight চালাতে পারছি না।");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAssistantSetupLabel();
        if (pendingWriteSettingsAction != null && Settings.System.canWrite(this)) {
            String action = pendingWriteSettingsAction;
            pendingWriteSettingsAction = null;
            handleSpeech(action);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ROLE_REQUEST) {
            refreshAssistantSetupLabel();
            if (pendingAssistantStart
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                pendingAssistantStart = false;
                startListening();
            }
            if (resultCode == RESULT_OK) {
                status.setText("RS-কে Default Assistant হিসেবে নির্বাচন করেছেন, Boss।");
                if (!hasNotificationAccess()) requestNotificationAccess();
            } else {
                status.setText("Default Assistant পরিবর্তন করা হয়নি। চাইলে পরে সেটআপ করতে পারবেন।");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_START_LISTENING, false)) {
            startListening();
        }
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
