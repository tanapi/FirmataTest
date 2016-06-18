package com.example.firmatatest;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import org.shokai.firmata.ArduinoFirmata;
import org.shokai.firmata.ArduinoFirmataEventHandler;

public class MyApplication extends Application {
    private final String TAG = "MYAPPLICATION";

    private ArduinoFirmata arduino;

    @Override
    public void onCreate() {
        /** Called when the Application-class is first created. */
        Log.v(TAG,"--- onCreate() in ---");
    }

    @Override
    public void onTerminate() {
        /** This Method Called when this Application finished. */
        Log.v(TAG,"--- onTerminate() in ---");
    }

    public void setMainActivty(android.app.Activity context){
        //Firmata設定
        this.arduino = new ArduinoFirmata(context);
        final Activity self = context;
        arduino.setEventHandler(new ArduinoFirmataEventHandler(){
            public void onError(String errorMessage){
                Log.e(TAG, errorMessage);
            }
            public void onClose(){
                Log.v(TAG, "arduino closed");
                self.finish();
            }
        });
    }

    public ArduinoFirmata getArduino(){
        return arduino;
    }
}
