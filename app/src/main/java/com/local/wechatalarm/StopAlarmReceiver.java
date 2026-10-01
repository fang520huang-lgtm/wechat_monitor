package com.local.wechatalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopAlarmReceiver extends BroadcastReceiver {
    static final String ACTION_STOP = "com.local.wechatalarm.STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            AlarmController.stop(context);
        }
    }
}
