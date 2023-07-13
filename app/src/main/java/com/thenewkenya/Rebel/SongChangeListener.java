package com.thenewkenya.Rebel;

import androidx.annotation.NonNull;

public interface SongChangeListener {

    void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

    void onChanged(int position);
}
