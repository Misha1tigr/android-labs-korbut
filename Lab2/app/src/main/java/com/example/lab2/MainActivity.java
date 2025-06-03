package com.example.lab2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Завантаження фрагментів (один раз)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.input_fragment_container, new InputFragment())
                    .replace(R.id.result_fragment_container, new ResultFragment())
                    .commit();
        }
    }
}
