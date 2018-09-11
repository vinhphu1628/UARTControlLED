package com.example.xfoodz.uartcontrolled;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INTERVAL_BETWEEN_BLINKS_MS_1 = 500;
    private static final int INTERVAL_BETWEEN_BLINKS_MS_2[] = {500, 200, 2000};
    private static final int INTERVAL_BETWEEN_BLINKS_MS_4 = 20;
    private static final int INTERVAL_BETWEEN_BLINKS_MS_5[] = {500, 2000, 3000};

    private static final String UART_DEVICE_NAME = "UART0";
    private UartDevice mDevice;
    private static final int CHUNK_SIZE = 512;
    private Handler mInputHandler = new Handler();
    private Handler mAppStateHandler = new Handler();
    private int appState = 0;
    private int cur = 0;

    private boolean mLedStateG = false;
    private boolean mLedStateR = false;
    private boolean mLedStateB = false;
    private int count = 0;

    private int count2_1 = 0;
    private int count2_2 = 0;
    private int blink = 0;

    private int count3_1 = 0;

    private Handler mHandler = new Handler();
    private Gpio mLedGpioR;
    private Gpio mLedGpioG;
    private Gpio mLedGpioB;
    private static final String PWM_NAME = "PWM1";
    private Pwm mPwm;
    private boolean state = false;
    private int dutyCycle = 0;
    private boolean direction = true;
    private int ledState = 0;
    private Button button;
    private String data;
    private EditText key;
    private Button buttonSend;

    private Handler mHandlerR = new Handler();
    private Handler mHandlerG = new Handler();
    private Handler mHandlerB = new Handler();

    @Override
    protected void onStart() {
        super.onStart();
        // Begin listening for interrupt events
        try {
            mDevice.registerUartDeviceCallback(mUartCallback);
        } catch (IOException e) {
            Log.e(TAG, "Unable to register Uart Device!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Interrupt events no longer necessary
        mDevice.unregisterUartDeviceCallback(mUartCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PeripheralManager manager = PeripheralManager.getInstance();
        try {
            mDevice = manager.openUartDevice(UART_DEVICE_NAME);
            configureUartFrame(mDevice);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access UART device", e);
        }

        try {
            mPwm = manager.openPwm(PWM_NAME);
            initializePwm(mPwm);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access PWM", e);
        }

        try {
            String R = "BCM16";
            String G = "BCM12";
            String B = "BCM26";
            mLedGpioR = PeripheralManager.getInstance().openGpio(R);
            mLedGpioR.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioG = PeripheralManager.getInstance().openGpio(G);
            mLedGpioG.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioB = PeripheralManager.getInstance().openGpio(B);
            mLedGpioB.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(appState == 2) {
                    switch (count2_1) {
                        case 0:
                            blink = INTERVAL_BETWEEN_BLINKS_MS_2[0];
                            count2_1 = count2_1 + 1;
                            break;
                        case 1:
                            blink = INTERVAL_BETWEEN_BLINKS_MS_2[1];
                            count2_1 = count2_1 + 1;
                            break;
                        case 2:
                            blink = INTERVAL_BETWEEN_BLINKS_MS_2[2];
                            count2_1 = 0;
                            break;
                    }
                }
                else if(appState == 3){
                    switch (count3_1){
                        case 0:
                            try {
                                mPwm.setPwmDutyCycle(10);
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to set Duty Cycle!");
                            }
                            count3_1 = count3_1 + 1;
                            break;
                        case 1:
                            try {
                                mPwm.setPwmDutyCycle(20);
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to set Duty Cycle!");
                            }
                            count3_1 = count3_1 + 1;
                            break;
                        case 2:
                            try {
                                mPwm.setPwmDutyCycle(50);
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to set Duty Cycle!");
                            }
                            count3_1 = 0;
                            break;
                    }
                }
                if(appState == 4) state = !state;
            }
        });

        key = findViewById(R.id.key);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = key.getText().toString();
                try {
                    writeUartData(mDevice, str.getBytes());
                } catch (IOException e) {
                    Log.e(TAG, "Unable to write data!");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }

        if (mPwm != null) {
            try {
                mPwm.close();
                mPwm = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close PWM", e);
            }
        }
    }
    public void init1(){
        try {
            mLedGpioR.setValue(false);
            mLedGpioG.setValue(false);
            mLedGpioB.setValue(false);
        } catch (IOException e) {
            Log.e(TAG, "Unable to set LED state!");
        }
        count = 0;
        mHandler.post(mBlinkRunnable);
    }

    public void init2(){
        count2_1 = 0;
        count2_2 = 0;
        blink = INTERVAL_BETWEEN_BLINKS_MS_2[0];
        mHandler.post(mBlinkRunnable2);
    }

    public void init3(){
        count3_1 = 0;
        try {
            mPwm.setPwmDutyCycle(10);
        } catch (IOException e) {
            Log.e("Error","Unable to set Duty Cycle!");
        }
        try {
            mLedGpioR.setValue(true);
            mLedGpioG.setValue(true);
            mLedGpioB.setValue(false);
        } catch (IOException e) {
            Log.e("Error", "Unable to set LED state!");
        }
    }

    public void init4(){
        dutyCycle = 0;
        ledState = 0;
        direction = true;
        state = false;
        try {
            mPwm.setPwmDutyCycle(10);
        } catch (IOException e) {
            Log.e("Error","Unable to set Duty Cycle!");
        }
        mHandler.post(blinkAll);
    }

    public void init5(){
        try {
            mLedGpioR.setValue(false);
            mLedGpioG.setValue(false);
            mLedGpioB.setValue(false);
        } catch (IOException e) {
            Log.e(TAG, "Unable to set LED state!");
        }
        mLedStateR = false;
        mLedStateG = false;
        mLedStateB = false;
        mHandlerR.post(mBlinkRunnableR);
        mHandlerG.post(mBlinkRunnableG);
        mHandlerB.post(mBlinkRunnableB);
    }

    public void configureUartFrame(UartDevice uart) throws IOException {
        // Configure the UART port
        uart.setBaudrate(115200);
        uart.setDataSize(8);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(1);
    }

    public void readUartBuffer(UartDevice uart) throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int count;
        while ((count = uart.read(buffer, buffer.length)) > 0) {
            Log.d(TAG, "Read " + count + " bytes from peripheral");
            String str = new String(buffer);
            Log.d(TAG, str.substring(0,count));
            data = str.substring(0,count);
            if(data.equals("O")) {
                try {
                    mLedGpioR.setValue(true);
                    mLedGpioG.setValue(true);
                    mLedGpioB.setValue(false);
                } catch (IOException e) {
                    Log.e("Error", "Unable to set LED state!");
                }
                mInputHandler.post(receivingCmd);
            }
        }
    }

    public void writeUartData(UartDevice uart, byte[] data) throws IOException {
        byte[] buffer = data;
        int count = uart.write(buffer, buffer.length);
        Log.d(TAG, "Wrote " + count + " bytes to peripheral");
    }

    private UartDeviceCallback mUartCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            // Read available data from the UART device
            try {
                readUartBuffer(uart);
            } catch (IOException e) {
                Log.w(TAG, "Unable to access UART device", e);
            }

            // Continue listening for more interrupts
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    private Runnable receivingCmd = new Runnable() {
        @Override
        public void run() {
            switch (data){
                case "O":
                    cur = 0;
                    appState = 0;
                    try {
                        mLedGpioR.setValue(true);
                        mLedGpioG.setValue(true);
                        mLedGpioB.setValue(false);
                    } catch (IOException e) {
                        Log.e("Error", "Unable to set LED state!");
                    }
                    Log.d("Debug", "Waiting for command...");
                    break;
                case "1":
                    if(appState != 1){
                        Log.d("Debug", "1");
                        cur = 1;
                        mAppStateHandler.postDelayed(runApp,5000);
                        appState = 1;
                    }
                    break;
                case "2":
                    if(appState != 2){
                        Log.d("Debug", "2");
                        cur = 2;
                        mAppStateHandler.postDelayed(runApp,5000);
                        appState = 2;
                    }
                    break;
                case "3":
                    if(appState != 3){
                        Log.d("Debug", "3");
                        cur = 3;
                        mAppStateHandler.postDelayed(runApp,5000);
                        appState = 3;
                    }
                    break;
                case "4":
                    if(appState != 4){
                        Log.d("Debug", "4");
                        cur = 4;
                        mAppStateHandler.postDelayed(runApp,5000);
                        appState = 4;
                    }
                    break;
                case "5":
                    if(appState != 5){
                        Log.d("Debug", "5");
                        cur = 5;
                        mAppStateHandler.postDelayed(runApp,5000);
                        appState = 5;
                    }
                    break;
                case "F":
                    if(appState != 0) {
                        Log.d("Debug", "F");
                        cur = 0;
                        appState = 0;
                    }
                    else {
                        try {
                            mLedGpioR.setValue(true);
                            mLedGpioG.setValue(true);
                            mLedGpioB.setValue(false);
                        } catch (IOException e) {
                            Log.e("Error", "Unable to set LED state!");
                        }
                        Log.d("Debug", "Waiting for command...");
                    }
                    break;
                case "S":
                    Log.d("Debug", "S");
                    cur = 0;
                    appState = 0;
                    mAppStateHandler.postDelayed(setRed, 5000);
                    break;
                default:
                    Log.d("Debug", "Unknown command...");
                    break;
            }
            if(!data.equals("S")) mInputHandler.post(receivingCmd);
        }
    };

    public void initializePwm(Pwm pwm) throws IOException {
        pwm.setPwmFrequencyHz(120);
        pwm.setPwmDutyCycle(10);

        // Enable the PWM signal
        pwm.setEnabled(true);
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                switch(count){
                    case 0:
                        mLedStateR = true;
                        mLedStateG = true;
                        mLedStateB = true;

                        count++;
                        break;

                    case 1:
                        mLedStateR = false;
                        mLedStateG = true;
                        mLedStateB = true;
                        count++;
                        break;

                    case 2:
                        mLedStateR = true;
                        mLedStateG = false;
                        mLedStateB = true;
                        count++;
                        break;

                    case 3:
                        mLedStateR = false;
                        mLedStateG = false;
                        mLedStateB = true;
                        count++;
                        break;

                    case 4:
                        mLedStateR = true;
                        mLedStateG = true;
                        mLedStateB = false;
                        count++;
                        break;

                    case 5:
                        mLedStateR = false;
                        mLedStateG = true;
                        mLedStateB = false;
                        count++;
                        break;

                    case 6:
                        mLedStateR = true;
                        mLedStateG = false;
                        mLedStateB = false;
                        count++;
                        break;

                    case 7:
                        mLedStateR = false;
                        mLedStateG = false;
                        mLedStateB = false;
                        count++;
                        break;

                    default:
                }
                if(count == 8) count = 0;
                mLedGpioR.setValue(mLedStateR);
                mLedGpioG.setValue(mLedStateG);
                mLedGpioB.setValue(mLedStateB);
                if(appState == 1) mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS_1);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable mBlinkRunnable2 = new Runnable() {
        @Override
        public void run() {
            switch (count2_2) {
                case 0:
                    mLedStateR = true;
                    mLedStateG = true;
                    mLedStateB = true;
                    count2_2++;
                    break;

                case 1:
                    mLedStateR = false;
                    mLedStateG = true;
                    mLedStateB = true;
                    count2_2++;
                    break;

                case 2:
                    mLedStateR = true;
                    mLedStateG = false;
                    mLedStateB = true;
                    count2_2++;
                    break;

                case 3:
                    mLedStateR = false;
                    mLedStateG = false;
                    mLedStateB = true;
                    count2_2++;
                    break;

                case 4:
                    mLedStateR = true;
                    mLedStateG = true;
                    mLedStateB = false;
                    count2_2++;
                    break;

                case 5:
                    mLedStateR = false;
                    mLedStateG = true;
                    mLedStateB = false;
                    count2_2++;
                    break;

                case 6:
                    mLedStateR = true;
                    mLedStateG = false;
                    mLedStateB = false;
                    count2_2++;
                    break;

                case 7:
                    mLedStateR = false;
                    mLedStateG = false;
                    mLedStateB = false;
                    count2_2++;
                    break;

                default:
            }
            if (count2_2 == 8) count2_2 = 0;
            try {
                mLedGpioR.setValue(mLedStateR);
                mLedGpioG.setValue(mLedStateG);
                mLedGpioB.setValue(mLedStateB);
            } catch (IOException e) {
                Log.e(TAG, "Unable to set LED state!");
            }
            if(appState == 2) mHandler.postDelayed(mBlinkRunnable2, blink);
        }

    };

    private Runnable blinkAll = new Runnable() {
        @Override
        public void run() {
            try {
                mLedGpioR.setValue(false);
                mLedGpioG.setValue(false);
                mLedGpioB.setValue(false);
            } catch (IOException e) {
                Log.e(TAG, "Unable to set LED State!");
            }

            if (dutyCycle >= 50) {
                direction = false;
            } else if (dutyCycle <= 0) {
                direction = true;
            }
            if (direction) {
                dutyCycle++;
            } else dutyCycle--;

            if (state) {
                try {
                    mPwm.setPwmDutyCycle(0);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set Duty Cycle!");
                }
                ledState = 0;
                if(appState == 4) mHandler.post(blinkEach);
            } else {
                try {
                    mPwm.setPwmDutyCycle(dutyCycle);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set Duty Cycle!");
                }
                if(appState == 4) mHandler.postDelayed(blinkAll, INTERVAL_BETWEEN_BLINKS_MS_4);
            }
        }
    };

    private Runnable blinkEach = new Runnable() {
        @Override
        public void run() {
            if(dutyCycle == 0) {
                switch (ledState){
                    case 0:
                        try {
                            mLedGpioR.setValue(false);
                            mLedGpioG.setValue(true);
                            mLedGpioB.setValue(true);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to set LED State!");
                        }
                        break;
                    case 1:
                        try {
                            mLedGpioR.setValue(true);
                            mLedGpioG.setValue(false);
                            mLedGpioB.setValue(true);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to set LED State!");
                        }
                        break;
                    case 2:
                        try {
                            mLedGpioR.setValue(true);
                            mLedGpioB.setValue(true);
                            mLedGpioB.setValue(false);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to set LED State!");
                        }
                        break;
                    default:
                }
                ledState++;
            }
            if (dutyCycle >= 50) {
                direction = false;
            } else if (dutyCycle <= 0) {
                direction = true;
            }
            if (direction) {
                dutyCycle++;
            } else dutyCycle--;

            if (state) {
                try {
                    mPwm.setPwmDutyCycle(dutyCycle);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set Duty Cycle!");
                }
                Log.d("Debug", String.valueOf(ledState));
                if(ledState > 3){
                    state = false;
                    if(appState == 4) mHandler.post(blinkAll);
                }
                else {
                    if(appState == 4) mHandler.postDelayed(blinkEach, INTERVAL_BETWEEN_BLINKS_MS_4);
                }
            } else {
                try {
                    mPwm.setPwmDutyCycle(0);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set Duty Cycle!");
                }
                if(appState == 4) mHandler.post(blinkAll);
            }
        }
    };

    private Runnable mBlinkRunnableR = new Runnable() {
        @Override
        public void run() {
            try {
                mLedStateR = !mLedStateR;
                mLedGpioR.setValue(mLedStateR);
                if(appState == 5) mHandlerR.postDelayed(mBlinkRunnableR, INTERVAL_BETWEEN_BLINKS_MS_5[0]);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable mBlinkRunnableG = new Runnable() {
        @Override
        public void run() {
            try {
                mLedStateG = !mLedStateG;
                mLedGpioG.setValue(mLedStateG);
                if(appState == 5) mHandlerG.postDelayed(mBlinkRunnableG, INTERVAL_BETWEEN_BLINKS_MS_5[1]);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable mBlinkRunnableB = new Runnable() {
        @Override
        public void run() {
            try {
                mLedStateB = !mLedStateB;
                mLedGpioB.setValue(mLedStateB);
                if(appState == 5) mHandlerB.postDelayed(mBlinkRunnableB, INTERVAL_BETWEEN_BLINKS_MS_5[2]);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable app1 = new Runnable() {
        @Override
        public void run() {
            init1();
        }
    };

    private Runnable app2 = new Runnable() {
        @Override
        public void run() {
            init2();
        }
    };

    private Runnable app3 = new Runnable() {
        @Override
        public void run() {
            init3();
        }
    };

    private Runnable app4 = new Runnable() {
        @Override
        public void run() {
            init4();
        }
    };

    private Runnable app5 = new Runnable() {
        @Override
        public void run() {
            init5();
        }
    };

    private Runnable runApp = new Runnable() {
        @Override
        public void run() {
            reset();
            switch (cur){
                case 1:
                    mAppStateHandler.post(app1);
                    break;
                case 2:
                    mAppStateHandler.post(app2);
                    break;
                case 3:
                    mAppStateHandler.post(app3);
                    break;
                case 4:
                    mAppStateHandler.post(app4);
                    break;
                case 5:
                    mAppStateHandler.post(app5);
                    break;
                default:
            }
        }
    };

    public void reset() {
        try {
            mLedGpioR.setValue(false);
            mLedGpioG.setValue(false);
            mLedGpioB.setValue(true);
        } catch (IOException e) {
            Log.e(TAG, "Unable to set LED State!");
        }
        try {
            mPwm.setPwmFrequencyHz(120);
            mPwm.setPwmDutyCycle(10);
        } catch (IOException e) {
            Log.e(TAG, "Unable to set PWM!");
        }
    }

    private Runnable setRed = new Runnable() {
        @Override
        public void run() {
            try {
                mLedGpioR.setValue(false);
                mLedGpioG.setValue(true);
                mLedGpioB.setValue(true);
            } catch (IOException e) {
                Log.e("Error", "Unable to set LED state!");
            }
        }
    };
}