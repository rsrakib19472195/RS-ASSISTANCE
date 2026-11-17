package com.rsassistance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

/** Opens RS and starts one foreground speech turn when Android invokes the assistant. */
public class RSVoiceInteractionSession extends VoiceInteractionSession {
    public RSVoiceInteractionSession(Context context) {
        super(context);
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_START_LISTENING, true);
        startAssistantActivity(intent);
        hide();
    }
}
