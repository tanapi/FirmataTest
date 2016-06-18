package com.example.firmatatest;

import android.util.Xml;

import org.shokai.firmata.ArduinoFirmata;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;

public class ArduinoScenario {

    final static String TAG = "ArduinoScenario";

    public interface ArduinoScenarioCallbacks {
        public void callbackMethod(int parm);
    }

    private ArduinoScenarioCallbacks _ArduinoScenarioCallbacksCallbacks;

    public void setCallbacks(ArduinoScenarioCallbacks myClassCallbacks){
        _ArduinoScenarioCallbacksCallbacks = myClassCallbacks;
    }

    public static final int NOP = 0;
    public static final int RESET = 1;
    public static final int PIN_MODE = 10;
    public static final int DIGITAL_WRITE = 100;
    public static final int SLEEP = 1001;
    public static final int CALLBACK = 1002;
    public static final int GOTO = 1003;
    public static final int LOOP = 1004;

    private class ScenarioData {
        public int Command;
        public int Parm1;
        public int Parm2;

        public ScenarioData(int com, int p1, int p2){
            this.Command = com;
            this.Parm1 = p1;
            this.Parm2 = p2;
        }
    }

    private ArrayList<ScenarioData> ScenarioList = new ArrayList<ScenarioData>();
    private int pc = 0;
    private int loopCount = 0;

    public ArduinoScenario() {
    }

    public void SetScenarioNumber(int No){
        //あとでやる
    }

    public boolean LoadScenario(String xmlStrings){
        boolean result = true;
        ScenarioList.clear();

        try {
            XmlPullParser xpp = Xml.newPullParser();
            xpp.setInput(new StringReader(xmlStrings));

            int com = 0;
            int p1 = 0;
            int p2 = 0;
            int tagType = 0;
            for(int xe = xpp.getEventType(); xe != XmlPullParser.END_DOCUMENT; xe = xpp.next()){
                switch(xe){
                    case XmlPullParser.START_TAG:
                        switch(xpp.getName()){
                            case "command":
                                com = 0;
                                p1 = 0;
                                p2 = 0;
                                tagType = 1;
                                break;
                            case "parm1":
                                tagType = 2;
                                break;
                            case "parm2":
                                tagType = 3;
                                break;
                            case "line":
                                tagType = 999;
                                break;
                            default:
                                tagType = 0;
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        String txt = xpp.getText();
                        switch(tagType) {
                            case 1:
                                switch (txt) {
                                    case "RESET":
                                        com = RESET;
                                        break;
                                    case "PIN_MODE":
                                        com = PIN_MODE;
                                        break;
                                    case "DIGITAL_WRITE":
                                        com = DIGITAL_WRITE;
                                        break;
                                    case "SLEEP":
                                        com = SLEEP;
                                        break;
                                    case "CALLBACK":
                                        com = CALLBACK;
                                        break;
                                    case "GOTO":
                                        com = GOTO;
                                        break;
                                    case "LOOP":
                                        com = LOOP;
                                        break;
                                    default:
                                        com = NOP;
                                        break;
                                }
                                break;
                            case 2:
                                try {
                                    p1 = Integer.parseInt(txt);
                                } catch (Exception e) {
                                    p1 = 0;
                                }
                                break;
                            case 3:
                                switch (txt) {
                                    case "IN":
                                        p2 = 0;
                                        break;
                                    case "OUT":
                                        p2 = 1;
                                        break;
                                    case "ON":
                                        p2 = 1;
                                        break;
                                    case "OFF":
                                        p2 = 0;
                                        break;
                                    default:
                                        try {
                                            p2 = Integer.parseInt(txt);
                                        } catch (Exception e) {
                                            p2 = 0;
                                        }
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(xpp.getName().equals("line")) {
                            ScenarioList.add(new ScenarioData(com, p1, p2));
                        }
                        tagType = 0;
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }

        //読み込んだら制御系変数の初期化
        pc = 0;
        loopCount = 0;

        return result;
    }

    public boolean ExecScenario(ArduinoFirmata arduino){
        if(ScenarioList == null){
            return false;
        }

        if(ScenarioList.size() > pc){
            ScenarioData sc = ScenarioList.get(pc);

            switch (sc.Command) {
                case RESET:
                    arduino.reset();
                    break;
                case PIN_MODE:
                    if(sc.Parm2 == 0) {
                        arduino.pinMode(sc.Parm1, arduino.INPUT);
                    }else{
                        arduino.pinMode(sc.Parm1, arduino.OUTPUT);
                    }
                    break;
                case DIGITAL_WRITE:
                    arduino.digitalWrite(sc.Parm1, (sc.Parm2==0)?false:true);
                    break;
                case SLEEP:
                    long st = System.currentTimeMillis();
                    while(true){
                        if(System.currentTimeMillis() - st > sc.Parm1){
                            break;
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case CALLBACK:
                    _ArduinoScenarioCallbacksCallbacks.callbackMethod(sc.Parm1);
                    break;
                case GOTO:
                    pc = sc.Parm1 -1;
                    break;
                case LOOP:
                    if(loopCount < sc.Parm1){
                        pc = sc.Parm2 -1;
                        loopCount++;
                    }else{
                        loopCount = 0;
                    }
                default:
                    break;
            }

            pc++;
            return true;
        }else{
            pc=0;
            return false;
        }
    }
}

