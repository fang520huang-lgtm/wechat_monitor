package com.local.wechatalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebindReceiver extends BroadcastReceiver {
    private static final String TAG = "WeChatAlarmListener";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Starting listener monitor after: "
                + (intent == null ? "unknown" : intent.getAction()));
        ListenerMonitorService.ensureRunning(context);
    }
}
