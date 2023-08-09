package com.thenewkenya.Rebel;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
    private MainActivity mainActivity;

    public MyGestureListener(MainActivity activity) {
        mainActivity = activity;
    }
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // swipe right: switches to previous song
                    mainActivity.switchToPreviousSong();
                } else {
                    // swipe left: switches to next song
                    mainActivity.switchToNextSong();
                }
                return true;
            }
        }
        return false;
    }
}
