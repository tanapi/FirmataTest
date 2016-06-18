package com.example.firmatatest;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.shokai.firmata.ArduinoFirmata;

public class MyIntentService extends IntentService implements ArduinoScenario.ArduinoScenarioCallbacks {

    final static String TAG = "MyIntentService";

    private ArduinoFirmata arduino;
    private boolean mStop = false;

    public MyIntentService(){
        super("MyIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStop = true;
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        mStop = false;
        MyApplication app = (MyApplication)this.getApplication();
        arduino = app.getArduino();

        String scenarioData = intent.getStringExtra("scenarioData");
        ArduinoScenario Scenario = new ArduinoScenario();
        Scenario.LoadScenario(scenarioData);
        Scenario.setCallbacks(this);

        boolean rtn = true;
        while(rtn && !mStop){
            rtn = Scenario.ExecScenario(arduino);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public void callbackMethod(int parm){
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("parm", parm);
        broadcastIntent.setAction("MY_ACTION");
        getBaseContext().sendBroadcast(broadcastIntent);
    }
}