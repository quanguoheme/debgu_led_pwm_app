package com.usbsdk.sample;

import android.content.Context;
import android.util.AttributeSet;

import com.chibde.visualizer.BarVisualizer;

public class SharedBarVisualizer extends BarVisualizer {
    public SharedBarVisualizer(Context context) {
        super(context);
    }

    public SharedBarVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SharedBarVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWaveformData(byte[] waveform) {
        if (waveform == null) {
            bytes = null;
        } else {
            bytes = new byte[waveform.length];
            System.arraycopy(waveform, 0, bytes, 0, waveform.length);
        }
        invalidate();
    }
}
