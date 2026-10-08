package com.biim3210.mediaplayer.util;

import javafx.util.Duration;

public class TimeFormatter {
    public static String format(Duration d) {
        int totalSec = (int) d.toSeconds();
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}