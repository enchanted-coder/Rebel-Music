package com.thenewkenya.Rebel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;
import android.Manifest;

import org.jetbrains.annotations.NonNls;

public class MainActivity extends AppCompatActivity {

    private void checkReadStoragePermissions() {
        if(checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    private void showPermissionRationale() {
        AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setIcon(R.drawable.ic_folder);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.perm_rationale));
        builder.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final int READ_FILES_CODE = 2588;
                        requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}
                                , READ_FILES_CODE);
                    }
                });
        builder.setCanceledOnTouchOutside(false);
        try {
            builder.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    private void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    private void onPermissionGranted() {
        // activity
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkReadStoragePermissions();



    }


}