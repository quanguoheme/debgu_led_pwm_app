package com.usbsdk.sample;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;


import android.app.Activity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.goodchip.ledstrip.LedStripJni;

public class LedStripTestActivity extends Activity implements OnClickListener {
    MyHandler handler;
    LedStripJni ledstrip;
    	final  int LED_GAP_SWITCH= 30; //MS
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setTitle(getTitle());
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(FLAG_FULLSCREEN | FLAG_KEEP_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN);
		setContentView(R.layout.activity_led);

        ledstrip = LedStripJni.getLedstrip();

        // 采用数组循环方式批量绑定监听器，并增加空指针安全校验
        int[] buttonIds = new int[]{
                R.id.button_blue,
                R.id.button_red,
                R.id.button_green,
                R.id.button_flicker,
                R.id.button_lamp
        };

        for (int id : buttonIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
            } else {
                Log.w("LedStripTest", "Button with ID " + id + " not found in layout.");
            }
        }

        ledstrip.Init();

        handler = new MyHandler();

	//	ControlButtonUtil.initControlButtonView(this);
	}

	
    protected void onDestroy() {
        handler.removeMessages(0);
        handler.removeMessages(1);
        ledstrip.DeInit();
        super.onDestroy();

    }



    @Override
    public void onClick(View v) {
        Log.d("ds1","  onClick"+ v.getTransitionName());
        handler.removeMessages(0);//;
        handler.removeMessages(1);//;
        int value0[]={0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
        };///grb
        switch (v.getId()) {
            case R.id.button_blue:
                Log.d("ds1","button_blue onClick");
                int []value00 = {0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                        0x0000ff,0x0000ff,0x0000ff,0x0000ff,0x0000ff,
                };///grb
                ledstrip.sendData(value00);
                break;
            case R.id.button_red:
                int []value1  = {0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00,
                        0x00ff00,0x00ff00,0x00ff00,0x00ff00,0x00ff00
                };///grb
                ledstrip.sendData(value1);
                break;
            case R.id.button_green:
                int []value2  = {0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                };///grb
                ledstrip.sendData(value2);
                break;
            case R.id.button_lamp:
                ledstrip.sendData(value0);
                int []value3  = {0x0000ff,0};///grb
                ledstrip.sendData(value3);
                handler.sendMessageDelayed(handler.obtainMessage(0, 1, 0,null),500);
                break;
            case R.id.button_flicker:
                int []value4  = {0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,0xff0000};///grb
                ledstrip.sendData(value4);
                handler.sendMessageDelayed(handler.obtainMessage(1, 0, 0,null),500);
                break;
        }

    }


    private class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            int arg;
            switch (msg.what) {
                case 0:
                    //跑马灯
                    arg = msg.arg1+1;
                    int []value=new int[arg];
                    value[arg-1] = 0x0000ff;
                    ledstrip.sendData(value);
                    if(arg == 36)
                        arg = 0;
                    handler.sendMessageDelayed(handler.obtainMessage(0, arg, 0,null),500);
                    break;
                case 1:
                    //闪烁
                    if(msg.arg1==0){
                        int []value4  = {0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                                0xff0000,0xff0000,0xff0000,0xff0000,0xff0000,
                        };
                        arg = 1;
                        ledstrip.sendData(value4);
                    }else{
                        int []value4  =  {0,0,0,0,0,0,0,0,0,0,
                                0,0,0,0,0,0,0,0,0,0,
                                0,0,0,0,0,0,0,0,0,0,
                                0,0,0,0,0,0,0,0,0,0,
                                0,0,0,0,0,0,0,0,0,0,
                                0,0,0,0,0,0,0,0,0,0,
                        };///grb
                        arg = 0;
                        ledstrip.sendData(value4);
                    }
                    handler.sendMessageDelayed(handler.obtainMessage(1, arg, 0,null),100);
                default:
                    break;
            }
        }
    }

	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			return false;
		}
		return super.dispatchKeyEvent(event);
	}
	


}
