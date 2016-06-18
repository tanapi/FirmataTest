package com.example.firmatatest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


public class MyBroadcastReceiver extends BroadcastReceiver {
    public static Handler handler;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        int parm = bundle.getInt("parm");

        if(handler !=null){
            Message msg = new Message();

            Bundle data = new Bundle();
            data.putInt("parm", parm);
            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    public void registerHandler(Handler locationUpdateHandler) {
        handler = locationUpdateHandler;
    }
}