package com.local.wechatalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebindReceiver extends BroadcastReceiver {
    private static final String TAG = "WeChatAlarmListener";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Requesting listener rebind after: "
                + (intent == null ? "unknown" : intent.getAction()));
        WeChatNotificationListener.requestReconnect(context);
    }
}
