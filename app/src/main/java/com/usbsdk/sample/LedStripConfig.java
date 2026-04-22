package com.usbsdk.sample;

public final class LedStripConfig {

    public static final int DEFAULT_LED_COUNT = 7;

    private LedStripConfig() {
    }

    public static int getLedCount() {
        return DEFAULT_LED_COUNT;
    }
}
