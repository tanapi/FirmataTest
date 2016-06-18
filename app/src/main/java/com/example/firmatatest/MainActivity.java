package com.example.firmatatest;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "MainActivity";

    private IntentFilter intentFilter;
    private MyBroadcastReceiver receiver;
    private MyApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiver = new MyBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("MY_ACTION");
        registerReceiver(receiver, intentFilter);
        receiver.registerHandler(updateHandler);

        //Firmata設定
        app = (MyApplication)this.getApplication();
        app.setMainActivty(this);

        // ボタンを設定
        Button button1 = (Button)findViewById(R.id.buttonLight);
        button1.setOnClickListener(buttonClick);

        //Arduino接続
        try{
            app.getArduino().connect();
            Log.v(TAG, "Board Version : "+app.getArduino().getBoardVersion());
        }
        catch(IOException e){
            e.printStackTrace();
            finish();
        }
        catch(InterruptedException e){
            e.printStackTrace();
            finish();
        }
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.buttonLight:
                    InputStream is = getResources().openRawResource(R.raw.scenaro01);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }catch(Exception e){
                        Log.e(TAG, "XML Read Error");
                    }
                    startScenario(sb.toString());
                    break;
            }
        }
    };

    private void startScenario(String scenarioData) {
        Intent intent = new Intent(getBaseContext(), MyIntentService.class);
        intent.putExtra("scenarioData", scenarioData);
        startService(intent);
    }

    private Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            Integer callbackID = bundle.getInt("parm");
            Log.v(TAG, callbackID.toString());
        }
    };

}
