package com.example.lab5;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    // Компоненти інтерфейсу
    private TextView rawTiltText, smoothedTiltText, correctedTiltText;
    private ImageView arrowImage;
    private Button calibrateButton;

    // Сенсор та менеджер сенсорів
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;

    // Допоміжні змінні для обчислень
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    // Згладжене значення кута
    private float smoothedTilt = 0f;
    private final float alpha = 0.1f; // коефіцієнт фільтра згладжування

    // Зміщення після калібрування
    private float calibrationOffset = 0f;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Пошук елементів інтерфейсу
        rawTiltText = findViewById(R.id.rawTiltText);
        smoothedTiltText = findViewById(R.id.smoothedTiltText);
        correctedTiltText = findViewById(R.id.correctedTiltText);
        arrowImage = findViewById(R.id.arrowImage);
        calibrateButton = findViewById(R.id.calibrateButton);

        // Ініціалізація сенсора
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        // Завантаження збереженого значення калібрування
        prefs = getSharedPreferences("TiltPrefs", MODE_PRIVATE);
        calibrationOffset = prefs.getFloat("calibrationOffset", 0f);

        // Обробка натискання кнопки калібрування
        calibrateButton.setOnClickListener(v -> {
            // Зберігаємо поточний згладжений нахил як нову "нульову" точку
            calibrationOffset = smoothedTilt;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("calibrationOffset", calibrationOffset);
            editor.apply();
        });

        // Повідомлення, якщо сенсор не доступний
        if (rotationVectorSensor == null) {
            rawTiltText.setText("Сенсор недоступний.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Реєструємо слухача сенсора
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Відключаємо слухача при паузі
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Якщо дані з сенсора обертання
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Отримуємо матрицю обертання
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            // Отримуємо кути орієнтації (azimuth, pitch, roll)
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Отримуємо нахил по осі Y (pitch)
            float pitchRad = orientationAngles[1];
            float rawTilt = (float) Math.toDegrees(pitchRad);

            // Згладжування значення (експоненціальне усереднення)
            smoothedTilt = alpha * rawTilt + (1 - alpha) * smoothedTilt;

            // Коригуємо нахил з урахуванням калібрування
            float correctedTilt = -(smoothedTilt - calibrationOffset);

            // Оновлення текстових полів
            rawTiltText.setText(String.format("Сирий нахил: %.1f°", rawTilt));
            smoothedTiltText.setText(String.format("Згладжений нахил: %.1f°", smoothedTilt));
            correctedTiltText.setText(String.format("Поточний кут: %.1f°", correctedTilt));

            // Кольорове виділення кута в залежності від значення
            float absTilt = Math.abs(correctedTilt);
            if (absTilt >= 15) {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            } else if (absTilt >= 10) {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            }

            // Обертаємо стрілку для відображення нахилу
            arrowImage.setRotation(-correctedTilt);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не використовується в даному випадку
    }
}
