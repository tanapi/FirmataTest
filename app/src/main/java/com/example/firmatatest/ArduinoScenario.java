package com.example.firmatatest;

import android.util.Log;
import android.util.Xml;

import org.shokai.firmata.ArduinoFirmata;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

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
    public static final int DIGITAL_READ = 101;
    public static final int ANALOG_WRITE = 200;
    public static final int ANALOG_READ = 201;
    public static final int SLEEP = 1001;
    public static final int CALLBACK = 1002;
    public static final int GOTO = 1003;
    public static final int LOOP = 1004;
    public static final int LOOP_END = 1005;
    public static final int SET = 1051;
    public static final int SET_ADD = 1052;
    public static final int SET_SUB = 1053;
    public static final int SET_MLT = 1054;
    public static final int SET_DIV = 1055;
    public static final int SET_MOD = 1056;
    public static final int IF_EQ = 1100;
    public static final int IF_NE = 1101;
    public static final int IF_GT = 1102;
    public static final int IF_GTE = 1103;
    public static final int IF_LT = 1104;
    public static final int IF_LTE = 1105;
    public static final int ELSE = 1151;
    public static final int ENDIF = 1152;
    public static final int DIGITAL_PIN = 0x40000000;
    public static final int ANALOG_PIN = 0x20000000;
    public static final int PROGRAM_COUNT = 0x10000000;
    public static final int LOOP_COUNT = 0x10000001;
    public static final int LOOP_MAX = 0x10000002;
    public static final int LOOP_PC = 0x10000003;
    public static final int ADD = 0x10000004;
    public static final int SUB = 0x10000005;
    public static final int MLT = 0x10000006;
    public static final int DIV = 0x10000007;
    public static final int MOD = 0x10000008;
    public static final int VAR = 0x10000010;
    public static final int VALUE_MASK = 0x0FFFFFFF;

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

    //変数管理クラス
    private class ScenarioVariable {
        public int pc;

        public int loopCnt;
        public int loopMax;
        public int loopPc;
        public Stack<Integer> loopCntStk;
        public Stack<Integer> loopMaxStk;
        public Stack<Integer> loopPcStk;

        public int Add;
        public int Sub;
        public int Mlt;
        public int Div;
        public int Mod;
        public ArrayList<Integer> vars = new ArrayList<Integer>();

        public ScenarioVariable(){
            Clear();
        }

        public void Clear(){
            pc = 0;

            loopCnt = 0;
            loopMax = 0;
            loopPc = 0;
            loopCntStk= new Stack<Integer>();
            loopMaxStk = new Stack<Integer>();
            loopPcStk = new Stack<Integer>();

            Add = 0;
            Sub = 0;
            Mlt = 0;
            Div = 0;
            Mod = 0;
            vars.clear();
            for(int i=0;i<15;i++){
                vars.add(0);
            }
        }

        public int getVar(ArduinoFirmata arduino, int parm){
            int val = parm & VALUE_MASK;
            if(parm>=DIGITAL_PIN + 1 && parm<=DIGITAL_PIN+15){
                val = (arduino.digitalRead(parm - DIGITAL_PIN))?1:0;
            }else if(parm>=ANALOG_PIN+1 && parm<=ANALOG_PIN+15){
                val =arduino.analogRead(parm - ANALOG_PIN);
            }else if(parm>=VAR+1 && parm <=VAR+15) {
                val = vars.get(parm - (VAR + 1));
            }else if(parm == PROGRAM_COUNT){
                val = pc;
            }else if(parm == LOOP_COUNT){
                val = loopCnt;
            }else if(parm == LOOP_MAX){
                val = loopMax;
            }else if(parm == LOOP_PC){
                val = loopPc;
            }else if(parm == ADD){
                val = Add;
            }else if(parm == SUB){
                val = Sub;
            }else if(parm == MLT){
                val = Mlt;
            }else if(parm == DIV){
                val = Div;
            }else if(parm == MOD){
                val = Mod;
            }

            return val;
        }

        public void setVar(int parm, int val){
            if(parm>=VAR+1 && parm <=VAR+15) {
                vars.set(parm - (VAR + 1), val);
            }else if(parm == ADD){
                Add = val;
            }else if(parm == SUB){
                Sub = val;
            }else if(parm == MLT){
                Mlt = val;
            }else if(parm == DIV){
                Div = val;
            }else if(parm == MOD){
                Mod = val;
            }
        }

        public void loopPush(){
            loopCntStk.push(loopCnt);
            loopMaxStk.push(loopMax);
            loopPcStk.push(loopPc);
        }

        public void loopPop(){
            loopCnt = loopCntStk.pop();
            loopMax = loopMaxStk.pop();
            loopPc = loopPcStk.pop();
        }
    }

    private ArrayList<ScenarioData> ScenarioList = new ArrayList<ScenarioData>();

    private ScenarioVariable varStack = new ScenarioVariable();

    public ArduinoScenario() {
    }

    public void SetScenarioNumber(int No){
        //あとでやる
    }

    private int com_trans(String txt){
        int com = 0;

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
            case "DIGITAL_READ":
                com = DIGITAL_READ;
                break;
            case "ANALOG_WRITE":
                com = ANALOG_WRITE;
                break;
            case "ANALOG_READ":
                com = ANALOG_READ;
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
            case "LOOP_END":
                com = LOOP_END;
                break;
            case "SET":
                com = SET;
                break;
            case "SET_ADD":
                com = SET_ADD;
                break;
            case "SET_SUB":
                com = SET_SUB;
                break;
            case "SET_MLT":
                com = SET_MLT;
                break;
            case "SET_DIV":
                com = SET_DIV;
                break;
            case "SET_MOD":
                com = SET_MOD;
                break;
            case "IF_EQ":
                com = IF_EQ;
                break;
            case "IF_NE":
                com = IF_NE;
                break;
            case "IF_GT":
                com = IF_GT;
                break;
            case "IF_GTE":
                com = IF_GTE;
                break;
            case "IF_LT":
                com = IF_LT;
                break;
            case "IF_LTE":
                com = IF_LTE;
                break;
            case "ELSE":
                com = ELSE;
                break;
            case "ENDIF":
                com = ENDIF;
                break;
            default:
                com = NOP;
                break;
        }

        return com;
    }

    private int parm_trans(String txt){
        int p = 0;
        switch (txt) {
            case "DIGITAL":
                p = 100;
                break;
            case "OUT":
                p = 1;
                break;
            case "ON":
                p = 1;
                break;
            case "OFF":
                p = 0;
                break;

            case "DIGITAL1":
                p = DIGITAL_PIN  + 1;
                break;
            case "DIGITAL2":
                p = DIGITAL_PIN  + 2;
                break;
            case "DIGITAL3":
                p = DIGITAL_PIN  + 3;
                break;
            case "DIGITAL4":
                p = DIGITAL_PIN  + 4;
                break;
            case "DIGITAL5":
                p = DIGITAL_PIN  + 5;
                break;
            case "DIGITAL6":
                p = DIGITAL_PIN  + 6;
                break;
            case "DIGITAL7":
                p = DIGITAL_PIN  + 7;
                break;
            case "DIGITAL8":
                p = DIGITAL_PIN  + 8;
                break;
            case "DIGITAL9":
                p = DIGITAL_PIN  + 9;
                break;
            case "DIGITAL10":
                p = DIGITAL_PIN  + 10;
                break;
            case "DIGITAL11":
                p = DIGITAL_PIN  + 11;
                break;
            case "DIGITAL12":
                p = DIGITAL_PIN  + 12;
                break;
            case "DIGITAL13":
                p = DIGITAL_PIN  + 13;
                break;
            case "DIGITAL14":
                p = DIGITAL_PIN  + 14;
                break;
            case "DIGITAL15":
                p = DIGITAL_PIN  + 15;
                break;

            case "ANALOG1":
                p = ANALOG_PIN  + 1;
                break;
            case "ANALOG2":
                p = ANALOG_PIN  + 2;
                break;
            case "ANALOG3":
                p = ANALOG_PIN  + 3;
                break;
            case "ANALOG4":
                p = ANALOG_PIN  + 4;
                break;
            case "ANALOG5":
                p = ANALOG_PIN  + 5;
                break;
            case "ANALOG6":
                p = ANALOG_PIN  + 6;
                break;
            case "ANALOG7":
                p = ANALOG_PIN  + 7;
                break;
            case "ANALOG8":
                p = ANALOG_PIN  + 8;
                break;
            case "ANALOG9":
                p = ANALOG_PIN  + 9;
                break;
            case "ANALOG10":
                p = ANALOG_PIN  + 10;
                break;
            case "ANALOG11":
                p = ANALOG_PIN  + 11;
                break;
            case "ANALOG12":
                p = ANALOG_PIN  + 12;
                break;
            case "ANALOG13":
                p = ANALOG_PIN  + 13;
                break;
            case "ANALOG14":
                p = ANALOG_PIN  + 14;
                break;
            case "ANALOG15":
                p = ANALOG_PIN  + 15;
                break;

            case "PROGRAM_COUNT":
                p = PROGRAM_COUNT;
                break;
            case "LOOP_COUNT":
                p = LOOP_COUNT;
                break;
            case "ADD":
                p = ADD;
                break;
            case "SUB":
                p = SUB;
                break;
            case "MLT":
                p = MLT;
                break;
            case "DIV":
                p = DIV;
                break;
            case "MOD":
                p = MOD;
                break;

            case "VAR1":
                p = VAR + 1;
                break;
            case "VAR2":
                p = VAR + 2;
                break;
            case "VAR3":
                p = VAR + 3;
                break;
            case "VAR4":
                p = VAR + 4;
                break;
            case "VAR5":
                p = VAR + 5;
                break;
            case "VAR6":
                p = VAR + 6;
                break;
            case "VAR7":
                p = VAR + 7;
                break;
            case "VAR8":
                p = VAR + 8;
                break;
            case "VAR9":
                p = VAR + 9;
                break;
            case "VAR10":
                p = VAR + 10;
                break;
            case "VAR11":
                p = VAR + 11;
                break;
            case "VAR12":
                p = VAR + 12;
                break;
            case "VAR13":
                p = VAR + 13;
                break;
            case "VAR14":
                p = VAR + 14;
                break;
            case "VAR15":
                p = VAR + 15;
                break;

            default:
                try {
                    p = Integer.parseInt(txt);
                } catch (Exception e) {
                    p = 0;
                }
        }

        return p;
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
                                com = com_trans(txt);
                                break;
                            case 2:
                                p1 = parm_trans(txt);
                                break;
                            case 3:
                                p2 = parm_trans(txt);
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

        //読み込んだら変数の初期化
        varStack.Clear();

        return result;
    }

    private void jumpIfEnd(){
        int nextPc;
        for(nextPc=varStack.pc+1;nextPc<ScenarioList.size();nextPc++){
            if(ScenarioList.get(nextPc).Command == ELSE  ||
                    ScenarioList.get(nextPc).Command == ENDIF){
                break;
            }
        }
        varStack.pc = nextPc;
    }

    public boolean ExecScenario(ArduinoFirmata arduino){
        if(ScenarioList == null){
            return false;
        }

        if(ScenarioList.size() > varStack.pc){
            ScenarioData sc = ScenarioList.get(varStack.pc);
            int p1 = varStack.getVar(arduino, sc.Parm1);
            int p2 = varStack.getVar(arduino, sc.Parm2);
            Log.v(TAG, "Exec pc=" + varStack.pc +"["+sc.Command+","+p1+","+p2+"]");

            switch (sc.Command) {
                case RESET:
                    arduino.reset();
                    break;
                case PIN_MODE:
                    if(p2 == 0) {
                        arduino.pinMode(p1, arduino.INPUT);
                    }else{
                        arduino.pinMode(p1, arduino.OUTPUT);
                    }
                    break;
                case DIGITAL_WRITE:
                    arduino.digitalWrite(p1, (p2==0)?false:true);
                    break;
                case SLEEP:
                    long st = System.currentTimeMillis();
                    while(true){
                        if(System.currentTimeMillis() - st > p1){
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
                    _ArduinoScenarioCallbacksCallbacks.callbackMethod(p1);
                    break;
                case GOTO:
                    varStack.pc = p1 -1;
                    break;
                case LOOP:
                    varStack.loopPush();

                    varStack.loopCnt = 0;
                    varStack.loopMax = p1;
                    varStack.loopPc = varStack.pc;
                    break;
                case LOOP_END:
                    if(varStack.loopCnt < varStack.loopMax -1){
                        varStack.pc = varStack.loopPc;
                        varStack.loopCnt++;
                    }else{
                        varStack.loopPop();
                    }
                    break;
                case SET:
                    varStack.setVar(sc.Parm1, p2);
                    break;
                case SET_ADD:
                    varStack.setVar(ADD, p1 + p2);
                    break;
                case SET_SUB:
                    varStack.setVar(SUB, p1 - p2);
                    break;
                case SET_MLT:
                    varStack.setVar(MLT, p1 * p2);
                    break;
                case SET_DIV:
                    varStack.setVar(DIV, p1 / p2);
                    break;
                case SET_MOD:
                    varStack.setVar(MOD, p1 % p2);
                    break;
                case IF_EQ:
                    if(p1 == p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case IF_NE:
                    if(p1 != p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case IF_GT:
                    if(p1 > p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case IF_GTE:
                    if(p1 >= p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case IF_LT:
                    if(p1 < p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case IF_LTE:
                    if(p1 <= p2){
                    }else{
                        jumpIfEnd();
                    }
                    break;
                case ELSE:
                    jumpIfEnd();
                    break;
                case ENDIF:
                    break;
                default:
                    break;
            }

            varStack.pc++;
            return true;
        }else{
            varStack.pc=0;
            return false;
        }
    }
}

