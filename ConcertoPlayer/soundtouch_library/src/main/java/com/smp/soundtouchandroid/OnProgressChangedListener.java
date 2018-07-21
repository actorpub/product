package com.smp.soundtouchandroid;

public interface OnProgressChangedListener {
    void onProgressChanged(int track, double currentPercentage, long position);
    void onProgressChanged(int track, long current, long total);
    void onTrackEnd(int track);

    void onExceptionThrown(String string);
}