package com.rsassistance;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

/**
 * Optional, user-enabled accessibility helper. It only attempts a WhatsApp send
 * after the user confirms the exact recipient and message inside RS.
 */
public class RsWhatsAppSendAccessibilityService extends AccessibilityService {
    private static final long PENDING_TTL_MS = 120_000L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();
        if (!"com.whatsapp".equals(packageName)
                && !"com.whatsapp.w4b".equals(packageName)) return;

        android.content.SharedPreferences prefs =
                getSharedPreferences("rs_notification_store", MODE_PRIVATE);
        String raw = prefs.getString("pending_whatsapp_send", "");
        if (raw.isEmpty()) return;

        JSONObject pending;
        try {
            pending = new JSONObject(raw);
        } catch (Exception e) {
            prefs.edit().remove("pending_whatsapp_send").apply();
            return;
        }

        long createdAt = pending.optLong("created_at", 0L);
        if (createdAt <= 0L || System.currentTimeMillis() - createdAt > PENDING_TTL_MS) {
            prefs.edit().remove("pending_whatsapp_send").apply();
            return;
        }

        String recipient = pending.optString("recipient", "").trim();
        String message = pending.optString("message", "");
        if (recipient.isEmpty() || message.isEmpty()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // Never send unless both the intended chat header and exact draft are visible.
        if (!treeContainsText(root, recipient)) return;
        if (!treeContainsExactEditText(root, message)) return;

        AccessibilityNodeInfo sendButton = findSendButton(root);
        if (sendButton == null) return;

        // Clear the one-shot authorization before clicking to prevent duplicate sends.
        prefs.edit().remove("pending_whatsapp_send").apply();
        sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean treeContainsText(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if (matches(text, expected) || matches(description, expected)) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (treeContainsText(node.getChild(i), expected)) return true;
        }
        return false;
    }

    private boolean treeContainsExactEditText(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence className = node.getClassName();
        if (className != null && className.toString().toLowerCase().contains("edittext")
                && normalize(text).equals(normalize(expected))) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (treeContainsExactEditText(node.getChild(i), expected)) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String viewId = node.getViewIdResourceName();
        CharSequence description = node.getContentDescription();
        CharSequence text = node.getText();
        String id = viewId == null ? "" : viewId.toLowerCase();
        String label = ((description == null ? "" : description.toString()) + " "
                + (text == null ? "" : text.toString())).toLowerCase();

        boolean sendControl = id.endsWith("/send") || id.contains(":id/send")
                || label.equals("send") || label.contains("send message")
                || label.contains("পাঠান") || label.contains("পাঠিয়ে দিন");
        if (sendControl && node.isClickable()) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findSendButton(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private boolean matches(CharSequence value, String expected) {
        if (value == null) return false;
        return value.toString().trim().equalsIgnoreCase(expected.trim());
    }

    private String normalize(CharSequence value) {
        if (value == null) return "";
        return value.toString().trim().replaceAll("\\s+", " ");
    }

    @Override
    public void onInterrupt() {
        // No continuous recording; this service only reacts to WhatsApp UI events.
    }
}
