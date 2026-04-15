package com.usbsdk.sample;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

import com.goodchip.ledstrip.LedStripJni;

import java.util.Random;

public class LedStripTestActivity extends Activity implements OnClickListener {
    private static final String TAG = "LedStripTest";

    private static final int LED_COUNT = 50;
    private static final int LED_GAP_SWITCH = 30; // ms
    private static final int AUTO_EFFECT_DURATION_MS = 10000;
    private static final int RANDOM_FRAME_INTERVAL_MS = 150;

    private static final int MSG_EFFECT_FRAME = 1;
    private static final int MSG_AUTO_NEXT = 2;
    private static final int REQUEST_RECORD_AUDIO = 1001;

    private static final int EFFECT_NONE = -1;
    private static final int EFFECT_MUSIC = 0;
    private static final int EFFECT_STATIC_WHITE = 1;
    private static final int EFFECT_BREATH = 2;
    private static final int EFFECT_FLICKER = 3;
    private static final int EFFECT_GRADIENT = 4;
    private static final int EFFECT_CHASE = 5;
    private static final int EFFECT_RAINBOW = 6;
    private static final int EFFECT_RANDOM = 7;
    private static final int EFFECT_RED = 8;
    private static final int EFFECT_GREEN = 9;
    private static final int EFFECT_BLUE = 10;

    private static final int[] AUTO_EFFECTS = new int[]{
            EFFECT_MUSIC,
            EFFECT_STATIC_WHITE,
            EFFECT_BREATH,
            EFFECT_FLICKER,
            EFFECT_GRADIENT,
            EFFECT_CHASE,
            EFFECT_RAINBOW,
            EFFECT_RANDOM,
            EFFECT_RED,
            EFFECT_GREEN,
            EFFECT_BLUE
    };

    private final Random random = new Random();

    private MyHandler handler;
    private LedStripJni ledstrip;
    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;

    private int currentEffect = EFFECT_NONE;
    private int frameIndex = 0;
    private int autoIndex = 0;
    private int musicLevel = 96;
    private boolean autoCycle = false;
    private boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getTitle());
        getWindow().addFlags(FLAG_FULLSCREEN | FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_led);

        handler = new MyHandler();
        ledstrip = LedStripJni.getLedstrip();
        bindButtons();

        ledstrip.Init();
        startAutoCycle();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        stopCurrentEffect();
        if (ledstrip != null) {
            ledstrip.sendData(createOffFrame());
            ledstrip.DeInit();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        autoCycle = false;
        startEffect(effectForButton(v.getId()), false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }

        if (currentEffect == EFFECT_MUSIC && mediaPlayer != null && hasRecordAudioPermission()) {
            setupVisualizer();
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    private void bindButtons() {
        int[] buttonIds = new int[]{
                R.id.button_blue,
                R.id.button_red,
                R.id.button_green,
                R.id.button_static_white,
                R.id.button_breath,
                R.id.button_flicker,
                R.id.button_gradient,
                R.id.button_lamp,
                R.id.button_rainbow,
                R.id.button_random,
                R.id.button_music
        };

        for (int i = 0; i < buttonIds.length; i++) {
            View view = findViewById(buttonIds[i]);
            if (view != null) {
                view.setOnClickListener(this);
            } else {
                Log.w(TAG, "Button with ID " + buttonIds[i] + " not found in layout.");
            }
        }
    }

    private int effectForButton(int id) {
        switch (id) {
            case R.id.button_blue:
                return EFFECT_BLUE;
            case R.id.button_red:
                return EFFECT_RED;
            case R.id.button_green:
                return EFFECT_GREEN;
            case R.id.button_static_white:
                return EFFECT_STATIC_WHITE;
            case R.id.button_breath:
                return EFFECT_BREATH;
            case R.id.button_flicker:
                return EFFECT_FLICKER;
            case R.id.button_gradient:
                return EFFECT_GRADIENT;
            case R.id.button_lamp:
                return EFFECT_CHASE;
            case R.id.button_rainbow:
                return EFFECT_RAINBOW;
            case R.id.button_random:
                return EFFECT_RANDOM;
            case R.id.button_music:
                return EFFECT_MUSIC;
            default:
                return EFFECT_STATIC_WHITE;
        }
    }

    private void startAutoCycle() {
        autoCycle = true;
        autoIndex = 0;
        startEffect(AUTO_EFFECTS[autoIndex], true);
    }

    private void goToNextAutoEffect() {
        if (!autoCycle || destroyed) {
            return;
        }

        autoIndex++;
        if (autoIndex >= AUTO_EFFECTS.length) {
            autoIndex = 0;
        }
        startEffect(AUTO_EFFECTS[autoIndex], true);
    }

    private void startEffect(int effect, boolean fromAutoCycle) {
        stopCurrentEffect();
        currentEffect = effect;
        frameIndex = 0;
        musicLevel = 96;

        if (effect == EFFECT_MUSIC) {
            startMusicEffect(fromAutoCycle);
            sendEffectFrame();
            scheduleNextEffectFrame();
            return;
        }

        sendEffectFrame();
        if (isDynamicEffect(effect)) {
            scheduleNextEffectFrame();
        }
        if (fromAutoCycle && autoCycle) {
            handler.sendEmptyMessageDelayed(MSG_AUTO_NEXT, AUTO_EFFECT_DURATION_MS);
        }
    }

    private void stopCurrentEffect() {
        if (handler != null) {
            handler.removeMessages(MSG_EFFECT_FRAME);
            handler.removeMessages(MSG_AUTO_NEXT);
        }
        releaseVisualizer();
        releaseMediaPlayer();
        currentEffect = EFFECT_NONE;
    }

    private boolean isDynamicEffect(int effect) {
        return effect == EFFECT_BREATH
                || effect == EFFECT_FLICKER
                || effect == EFFECT_GRADIENT
                || effect == EFFECT_CHASE
                || effect == EFFECT_RAINBOW
                || effect == EFFECT_RANDOM
                || effect == EFFECT_MUSIC;
    }

    private void scheduleNextEffectFrame() {
        if (handler == null || currentEffect == EFFECT_NONE || !isDynamicEffect(currentEffect)) {
            return;
        }

        int delay = currentEffect == EFFECT_RANDOM ? RANDOM_FRAME_INTERVAL_MS : LED_GAP_SWITCH;
        handler.sendEmptyMessageDelayed(MSG_EFFECT_FRAME, delay);
    }

    private void sendEffectFrame() {
        if (ledstrip == null || currentEffect == EFFECT_NONE) {
            return;
        }

        ledstrip.sendData(createFrameForEffect(currentEffect));
    }

    private int[] createFrameForEffect(int effect) {
        switch (effect) {
            case EFFECT_MUSIC:
                return createMusicFrame();
            case EFFECT_STATIC_WHITE:
                return createSolidFrame(packRgb(255, 255, 255));
            case EFFECT_BREATH:
                return createBreathFrame();
            case EFFECT_FLICKER:
                return createFlickerFrame();
            case EFFECT_GRADIENT:
                return createGradientFrame();
            case EFFECT_CHASE:
                return createChaseFrame();
            case EFFECT_RAINBOW:
                return createRainbowFrame();
            case EFFECT_RANDOM:
                return createRandomFrame();
            case EFFECT_RED:
                return createSolidFrame(packRgb(255, 0, 0));
            case EFFECT_GREEN:
                return createSolidFrame(packRgb(0, 255, 0));
            case EFFECT_BLUE:
                return createSolidFrame(packRgb(0, 0, 255));
            default:
                return createOffFrame();
        }
    }

    private int[] createSolidFrame(int color) {
        int[] data = new int[LED_COUNT];
        for (int i = 0; i < LED_COUNT; i++) {
            data[i] = color;
        }
        return data;
    }

    private int[] createOffFrame() {
        return new int[LED_COUNT];
    }

    private int[] createBreathFrame() {
        double wave = (Math.sin(frameIndex * Math.PI / 33.0 - Math.PI / 2.0) + 1.0) / 2.0;
        int level = clamp((int) (wave * 255));
        return createSolidFrame(packRgb(level, level, level));
    }

    private int[] createFlickerFrame() {
        boolean on = ((frameIndex / 3) % 2) == 0;
        return createSolidFrame(on ? packRgb(255, 0, 0) : 0);
    }

    private int[] createGradientFrame() {
        int[][] palette = new int[][]{
                {255, 0, 0},
                {0, 255, 0},
                {0, 80, 255},
                {180, 0, 255}
        };
        int[] data = new int[LED_COUNT];
        for (int i = 0; i < LED_COUNT; i++) {
            float position = ((i * palette.length) / (float) LED_COUNT) + frameIndex * 0.02f;
            int base = ((int) Math.floor(position)) % palette.length;
            int next = (base + 1) % palette.length;
            float ratio = position - (float) Math.floor(position);
            data[i] = interpolateRgb(palette[base], palette[next], ratio);
        }
        return data;
    }

    private int[] createChaseFrame() {
        int[] data = new int[LED_COUNT];
        int head = frameIndex % LED_COUNT;
        for (int tail = 0; tail < 5; tail++) {
            int index = head - tail;
            if (index < 0) {
                index += LED_COUNT;
            }
            int level = clamp(255 - tail * 45);
            data[index] = packRgb(level / 3, level / 2, level);
        }
        return data;
    }

    private int[] createRainbowFrame() {
        int[] data = new int[LED_COUNT];
        for (int i = 0; i < LED_COUNT; i++) {
            float hue = (i * 360.0f / LED_COUNT + frameIndex * 4.0f) % 360.0f;
            data[i] = packHsv(hue, 1.0f, 1.0f);
        }
        return data;
    }

    private int[] createRandomFrame() {
        int[] data = new int[LED_COUNT];
        for (int i = 0; i < LED_COUNT; i++) {
            int red = 40 + random.nextInt(216);
            int green = 40 + random.nextInt(216);
            int blue = 40 + random.nextInt(216);
            data[i] = packRgb(red, green, blue);
        }
        return data;
    }

    private int[] createMusicFrame() {
        if (visualizer == null && mediaPlayer != null) {
            int position = mediaPlayer.getCurrentPosition();
            double pulse = Math.abs(Math.sin(position / 180.0));
            musicLevel = clamp(40 + (int) (pulse * 215));
        }

        int level = clamp(musicLevel);
        int litCount = Math.max(1, level * LED_COUNT / 255);
        float value = 0.25f + (level / 255.0f) * 0.75f;
        float baseHue = (frameIndex * 4.0f + level) % 360.0f;
        int[] data = new int[LED_COUNT];

        for (int i = 0; i < LED_COUNT; i++) {
            if (i < litCount) {
                data[i] = packHsv((baseHue + i * 7.0f) % 360.0f, 1.0f, value);
            } else {
                data[i] = 0;
            }
        }
        return data;
    }

    private int interpolateRgb(int[] start, int[] end, float ratio) {
        int red = clamp((int) (start[0] + (end[0] - start[0]) * ratio));
        int green = clamp((int) (start[1] + (end[1] - start[1]) * ratio));
        int blue = clamp((int) (start[2] + (end[2] - start[2]) * ratio));
        return packRgb(red, green, blue);
    }

    private int packHsv(float hue, float saturation, float value) {
        int color = Color.HSVToColor(new float[]{hue, saturation, value});
        return packRgb(Color.red(color), Color.green(color), Color.blue(color));
    }

    private int packRgb(int red, int green, int blue) {
        return (clamp(blue) << 16) | (clamp(red) << 8) | clamp(green);
    }

    private int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }

    private void startMusicEffect(boolean fromAutoCycle) {
        requestRecordAudioPermissionIfNeeded();

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.output_half);
            if (mediaPlayer == null) {
                Log.w(TAG, "MediaPlayer.create returned null.");
                if (autoCycle) {
                    handler.sendEmptyMessageDelayed(MSG_AUTO_NEXT, AUTO_EFFECT_DURATION_MS);
                }
                return;
            }

            mediaPlayer.setLooping(!fromAutoCycle);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (autoCycle && currentEffect == EFFECT_MUSIC && !destroyed) {
                        goToNextAutoEffect();
                    }
                }
            });
            mediaPlayer.start();

            if (hasRecordAudioPermission()) {
                setupVisualizer();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to start music effect.", e);
            releaseMediaPlayer();
            if (autoCycle) {
                handler.sendEmptyMessageDelayed(MSG_AUTO_NEXT, AUTO_EFFECT_DURATION_MS);
            }
        }
    }

    private void setupVisualizer() {
        releaseVisualizer();
        if (mediaPlayer == null || !hasRecordAudioPermission()) {
            return;
        }

        try {
            int audioSessionId = mediaPlayer.getAudioSessionId();
            if (audioSessionId == 0) {
                return;
            }

            visualizer = new Visualizer(audioSessionId);
            int[] captureSizeRange = Visualizer.getCaptureSizeRange();
            visualizer.setCaptureSize(captureSizeRange[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    if (waveform == null || waveform.length == 0) {
                        return;
                    }

                    int sum = 0;
                    for (int i = 0; i < waveform.length; i++) {
                        int sample = (waveform[i] & 0xff) - 128;
                        sum += Math.abs(sample);
                    }
                    musicLevel = clamp((sum / waveform.length) * 4);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    // Waveform energy is enough for this demo.
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Failed to setup visualizer, fallback to simulated beat.", e);
            releaseVisualizer();
        }
    }

    private void releaseVisualizer() {
        if (visualizer == null) {
            return;
        }

        try {
            visualizer.setEnabled(false);
        } catch (Exception ignored) {
        }
        try {
            visualizer.release();
        } catch (Exception ignored) {
        }
        visualizer = null;
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            mediaPlayer.setOnCompletionListener(null);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (Exception ignored) {
        }
        try {
            mediaPlayer.release();
        } catch (Exception ignored) {
        }
        mediaPlayer = null;
    }

    private boolean hasRecordAudioPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasRecordAudioPermission()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (destroyed) {
                return;
            }

            switch (msg.what) {
                case MSG_EFFECT_FRAME:
                    frameIndex++;
                    sendEffectFrame();
                    scheduleNextEffectFrame();
                    break;
                case MSG_AUTO_NEXT:
                    goToNextAutoEffect();
                    break;
                default:
                    break;
            }
        }
    }
}
