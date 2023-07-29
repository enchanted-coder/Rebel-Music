package com.thenewkenya.Rebel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PermissionRationale extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_rationale);

        Button button = findViewById(R.id.reqOk);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            goBack();
        }
        if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            goBack();
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int READ_FILES_CODE = 2588;

                if (Utils.isTiramisu()) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_AUDIO}
                            , READ_FILES_CODE);
                    recreate();
                } else if (Utils.isMarshmallow()){
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                            , READ_FILES_CODE);
                    recreate();
                    
                }

            }


        });





    }

    private void goBack() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


}