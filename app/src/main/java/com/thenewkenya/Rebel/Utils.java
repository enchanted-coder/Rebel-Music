package com.thenewkenya.Rebel;

import android.os.Build;

public class Utils {
    static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    static boolean isTiramisu() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
