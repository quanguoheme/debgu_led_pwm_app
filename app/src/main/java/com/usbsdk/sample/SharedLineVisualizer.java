package com.usbsdk.sample;

import android.content.Context;
import android.util.AttributeSet;

import com.chibde.visualizer.LineVisualizer;

public class SharedLineVisualizer extends LineVisualizer {
    public SharedLineVisualizer(Context context) {
        super(context);
    }

    public SharedLineVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SharedLineVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
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
