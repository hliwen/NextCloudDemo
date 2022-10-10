package com.example.nextclouddemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.nextclouddemo.utils.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("MainActivitylog", "onReceive: intent.getAction() ="+intent.getAction());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, MainActivity.class);
        context.startActivity(intent);
    }
}