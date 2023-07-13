package com.thenewkenya.Rebel;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



    }


}