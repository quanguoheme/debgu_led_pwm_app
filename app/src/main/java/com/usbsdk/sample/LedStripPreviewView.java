package com.usbsdk.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class LedStripPreviewView extends View {
    private static final int DEFAULT_LED_COUNT = 50;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] colors = new int[DEFAULT_LED_COUNT];

    public LedStripPreviewView(Context context) {
        super(context);
    }

    public LedStripPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LedStripPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLedData(int[] data) {
        if (data == null || data.length == 0) {
            colors = new int[DEFAULT_LED_COUNT];
        } else {
            colors = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                colors[i] = toAndroidColor(data[i]);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int count = colors.length;
        if (count == 0) {
            return;
        }

        float gap = Math.max(1.0f, getWidth() / 400.0f);
        float ledWidth = (getWidth() - gap * (count - 1)) / count;
        float top = getPaddingTop();
        float bottom = getHeight() - getPaddingBottom();

        for (int i = 0; i < count; i++) {
            float left = i * (ledWidth + gap);
            rect.set(left, top, left + ledWidth, bottom);
            paint.setColor(colors[i]);
            canvas.drawRoundRect(rect, 4.0f, 4.0f, paint);
        }
    }

    private int toAndroidColor(int ledColor) {
        int blue = (ledColor >> 16) & 0xff;
        int red = (ledColor >> 8) & 0xff;
        int green = ledColor & 0xff;
        return Color.rgb(red, green, blue);
    }
}
