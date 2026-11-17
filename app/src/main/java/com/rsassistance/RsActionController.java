package com.rsassistance;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.provider.AlarmClock;
import android.provider.Settings;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Executes a limited set of safe, on-device actions from explicit voice commands. */
public final class RsActionController {
    public static final class Result {
        public final boolean handled;
        public final boolean needsCameraPermission;
        public final String message;

        private Result(boolean handled, boolean needsCameraPermission, String message) {
            this.handled = handled;
            this.needsCameraPermission = needsCameraPermission;
            this.message = message;
        }

        static Result notHandled() { return new Result(false, false, ""); }
        static Result permission() { return new Result(false, true, ""); }
        static Result done(String message) { return new Result(true, false, message); }
    }

    private RsActionController() {}

    public static Result execute(Activity activity, String spoken) {
        if (spoken == null) return Result.notHandled();
        String q = spoken.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return Result.notHandled();

        if (isFlashlightCommand(q)) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) return Result.permission();
            boolean turnOn = isFlashlightOnCommand(q);
            try {
                CameraManager manager = (CameraManager) activity.getSystemService(Activity.CAMERA_SERVICE);
                for (String id : manager.getCameraIdList()) {
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(flash)) {
                        manager.setTorchMode(id, turnOn);
                        return Result.done(turnOn
                                ? "ফ্ল্যাশলাইট জ্বালিয়ে দিয়েছি, Boss।"
                                : "ফ্ল্যাশলাইট বন্ধ করে দিয়েছি, Boss।");
                    }
                }
                return Result.done("এই ফোনে ব্যবহারযোগ্য ফ্ল্যাশলাইট খুঁজে পেলাম না, Boss।");
            } catch (CameraAccessException | RuntimeException e) {
                return Result.done("ফ্ল্যাশলাইট নিয়ন্ত্রণ করা যায়নি। ক্যামেরা অন্য কোনো অ্যাপ ব্যবহার করছে কি না দেখুন, Boss।");
            }
        }

        if (containsAny(q, "volume up", "sound up", "আওয়াজ বাড়াও", "ভলিউম বাড়াও",
                "শব্দ বাড়াও", "volume barao", "sound barao")) {
            adjustVolume(activity, AudioManager.ADJUST_RAISE);
            return Result.done("ভলিউম একটু বাড়িয়ে দিয়েছি, Boss।");
        }
        if (containsAny(q, "volume down", "sound down", "আওয়াজ কমাও", "ভলিউম কমাও",
                "শব্দ কমাও", "volume komao", "sound komao")) {
            adjustVolume(activity, AudioManager.ADJUST_LOWER);
            return Result.done("ভলিউম একটু কমিয়ে দিয়েছি, Boss।");
        }
        if (containsAny(q, "mute", "sound off", "নিঃশব্দ", "শব্দ বন্ধ", "সাউন্ড বন্ধ")) {
            adjustVolume(activity, AudioManager.ADJUST_MUTE);
            return Result.done("মিডিয়া সাউন্ড মিউট করেছি, Boss।");
        }
        if (containsAny(q, "unmute", "sound on", "শব্দ চালু", "সাউন্ড চালু")) {
            adjustVolume(activity, AudioManager.ADJUST_UNMUTE);
            return Result.done("মিডিয়া সাউন্ড আবার চালু করেছি, Boss।");
        }

        if (containsAny(q, "battery", "charge", "চার্জ", "ব্যাটারি")) {
            Intent battery = activity.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int percent = Math.round(level * 100f / scale);
                    return Result.done("Boss, আপনার ফোনে এখন " + percent + " শতাংশ চার্জ আছে।");
                }
            }
            return Result.done("এখন ব্যাটারির তথ্য পড়তে পারছি না, Boss।");
        }

        if (containsAny(q, "open settings", "settings kholo", "সেটিংস খোলো",
                "সেটিংস খুলে দাও", "সেটিংস চালু কর")) {
            return launchIntent(activity, new Intent(Settings.ACTION_SETTINGS),
                    "ফোনের Settings খুলে দিয়েছি, Boss।");
        }

        if (containsAny(q, "wifi settings", "wi-fi settings", "ওয়াইফাই সেটিংস")) {
            return launchIntent(activity, new Intent(Settings.ACTION_WIFI_SETTINGS),
                    "Wi-Fi Settings খুলে দিয়েছি, Boss।");
        }

        if (containsAny(q, "bluetooth settings", "ব্লুটুথ সেটিংস")) {
            return launchIntent(activity, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                    "Bluetooth Settings খুলে দিয়েছি, Boss।");
        }

        if (containsAny(q, "alarm", "অ্যালার্ম", "ঘড়িতে অ্যালার্ম")) {
            Matcher matcher = Pattern.compile("(?<!\\d)(\\d{1,2})(?::(\\d{2}))?(?!\\d)").matcher(q);
            if (matcher.find()) {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
                if (hour <= 23 && minute <= 59) {
                    Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM);
                    alarm.putExtra(AlarmClock.EXTRA_HOUR, hour);
                    alarm.putExtra(AlarmClock.EXTRA_MINUTES, minute);
                    alarm.putExtra(AlarmClock.EXTRA_MESSAGE, "RS reminder for Boss");
                    alarm.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    return launchIntent(activity, alarm,
                            String.format(Locale.getDefault(),
                                    "%02d:%02d সময়ের অ্যালার্ম সেট করার স্ক্রিন খুলে দিয়েছি, Boss। Clock app-এ Confirm করুন।",
                                    hour, minute));
                }
            }
            return Result.done("কয়টার অ্যালার্ম দেব, Boss? সময়টা যেমন ৭:৩০ বলুন।");
        }

        String target = extractAppTarget(q);
        if (target != null) return openApp(activity, target);

        return Result.notHandled();
    }

    private static boolean isFlashlightCommand(String q) {
        return containsAny(q, "flashlight", "torch", "ফ্ল্যাশলাইট", "টর্চ");
    }

    private static boolean isFlashlightOnCommand(String q) {
        return containsAny(q, "torch on", "flashlight on", "torch jalao",
                "flashlight jalao", "torch jalo", "flashlight jalo",
                "টর্চ জ্বাল", "ফ্ল্যাশলাইট জ্বাল", "টর্চ চালু", "ফ্ল্যাশলাইট চালু");
    }

    private static void adjustVolume(Activity activity, int direction) {
        AudioManager audio = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
        if (audio != null) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
    }

    private static Result launchIntent(Activity activity, Intent intent, String success) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(activity.getPackageManager()) == null) {
                return Result.done("এই কাজের জন্য প্রয়োজনীয় অ্যাপটি পাওয়া যায়নি, Boss।");
            }
            activity.startActivity(intent);
            return Result.done(success);
        } catch (Exception e) {
            return Result.done("এই মুহূর্তে কাজটি খুলতে পারলাম না, Boss।");
        }
    }

    private static String extractAppTarget(String q) {
        String target = q;
        String[] prefixes = {"please open ", "open ", "launch ", "start ",
                "অ্যাপ খোলো ", "অ্যাপ খুলে দাও "};
        for (String prefix : prefixes) {
            if (target.startsWith(prefix)) {
                target = target.substring(prefix.length()).trim();
                break;
            }
        }
        String[] suffixes = {" kholo", " khulo", " open koro", " chalu koro",
                " খোলো", " খুলে দাও", " চালু করো", " চালু কর"};
        for (String suffix : suffixes) {
            if (target.endsWith(suffix)) {
                target = target.substring(0, target.length() - suffix.length()).trim();
                break;
            }
        }
        if (target.equals(q)) return null;
        if (target.isEmpty()) return null;
        return target;
    }

    private static Result openApp(Activity activity, String target) {
        String wanted = target.toLowerCase(Locale.ROOT).trim();
        if (wanted.equals("হোয়াটসঅ্যাপ") || wanted.equals("হোয়াটসঅ্যাপ")) wanted = "whatsapp";
        if (wanted.equals("ফেসবুক")) wanted = "facebook";
        if (wanted.equals("ইউটিউব")) wanted = "youtube";
        if (wanted.equals("ক্রোম")) wanted = "chrome";

        PackageManager pm = activity.getPackageManager();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.MATCH_ALL);
            ApplicationInfo best = null;
            for (ApplicationInfo app : apps) {
                Intent launch = pm.getLaunchIntentForPackage(app.packageName);
                if (launch == null || app.packageName.equals(activity.getPackageName())) continue;
                String label = pm.getApplicationLabel(app).toString().toLowerCase(Locale.ROOT);
                if (label.equals(wanted) || app.packageName.toLowerCase(Locale.ROOT).contains(wanted)) {
                    best = app;
                    break;
                }
                if (best == null && (label.contains(wanted) || wanted.contains(label))) best = app;
            }
            if (best != null) {
                Intent launch = pm.getLaunchIntentForPackage(best.packageName);
                if (launch != null) {
                    activity.startActivity(launch);
                    return Result.done(pm.getApplicationLabel(best) + " খুলে দিয়েছি, Boss।");
                }
            }
        } catch (Exception ignored) {}
        return Result.done("Boss, '" + target + "' নামে কোনো চালু করা যায় এমন অ্যাপ খুঁজে পেলাম না।");
    }

    private static boolean containsAny(String value, String... parts) {
        for (String part : parts) if (value.contains(part)) return true;
        return false;
    }
}
