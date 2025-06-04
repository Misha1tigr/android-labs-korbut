package com.example.lab6;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements SensorEventListener, LocationListener {

    // Компоненти інтерфейсу
    private TextView rawTiltText, smoothedTiltText, correctedTiltText;
    private TextView rawGpsText, speedText;
    private ImageView arrowImage, speedArrowImage;
    private Button calibrateButton;

    // Компоненти датчика нахилу
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private float smoothedTilt = 0f;
    private final float alpha = 0.1f; // Коефіцієнт фільтра згладжування нахилу
    private float calibrationOffset = 0f;
    private SharedPreferences prefs;

    // GPS компоненти
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationManager locationManager;
    private boolean isGpsEnabled = false;
    private float smoothedSpeed = 0f;
    private final float speedAlpha = 0.4f; // Коефіцієнт згладжування швидкості (зменшено для швидшої реакції)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ініціалізація компонентів інтерфейсу
        rawTiltText = findViewById(R.id.rawTiltText);
        smoothedTiltText = findViewById(R.id.smoothedTiltText);
        correctedTiltText = findViewById(R.id.correctedTiltText);
        rawGpsText = findViewById(R.id.rawGpsText);
        speedText = findViewById(R.id.speedText);
        arrowImage = findViewById(R.id.arrowImage);
        speedArrowImage = findViewById(R.id.speedArrowImage);
        calibrateButton = findViewById(R.id.calibrateButton);

        // Ініціалізація датчика нахилу
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        // Ініціалізація GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Завантаження налаштувань калібрування
        prefs = getSharedPreferences("TiltPrefs", MODE_PRIVATE);
        calibrationOffset = prefs.getFloat("calibrationOffset", 0f);

        // Обробник кнопки калібрування
        calibrateButton.setOnClickListener(v -> {
            calibrationOffset = smoothedTilt;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("calibrationOffset", calibrationOffset);
            editor.apply();
        });

        // Перевірка доступності датчика
        if (rotationVectorSensor == null) {
            rawTiltText.setText("Сенсор недоступний.");
        }

        // Перевірка та запит дозволів GPS
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Потрібен дозвіл на використання GPS", Toast.LENGTH_LONG).show();
                rawGpsText.setText("GPS: Дозвіл відхилено");
            }
        }
    }

    private void startLocationUpdates() {
        if (!checkLocationPermission()) {
            return;
        }

        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            rawGpsText.setText("GPS: Вимкнено");
            speedText.setText("0 км/г");
            return;
        }

        rawGpsText.setText("GPS: Пошук сигналу...");

        // Запит оновлень місцезнаходження
        // Оновлення кожну секунду або при переміщенні на 1 метр
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 секунда
                1,    // 1 метр
                this
        );

        // Спроба отримати останнє відоме місцезнаходження
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            onLocationChanged(lastLocation);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Реєстрація слухача датчика нахилу
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }

        // Відновлення оновлень GPS
        if (checkLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Відключення датчиків
        sensorManager.unregisterListener(this);

        // Зупинка оновлень GPS
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Отримання матриці обертання
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Отримання кутів орієнтації
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Отримання кута нахилу
            float pitchRad = orientationAngles[1];
            float rawTilt = (float) Math.toDegrees(pitchRad);

            // Згладжування значення
            smoothedTilt = alpha * rawTilt + (1 - alpha) * smoothedTilt;

            // Застосування калібрування
            float correctedTilt = -(smoothedTilt - calibrationOffset);

            // Оновлення відображення нахилу
            rawTiltText.setText(String.format("Сирий нахил: %.1f°", rawTilt));
            smoothedTiltText.setText(String.format("Згладжений нахил: %.1f°", smoothedTilt));
            correctedTiltText.setText(String.format("Поточний кут: %.1f°", correctedTilt));

            // Кольорове кодування на основі кута нахилу
            float absTilt = Math.abs(correctedTilt);
            if (absTilt >= 15) {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            } else if (absTilt >= 10) {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                correctedTiltText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            }

            // Обертання стрілки для відображення нахилу
            arrowImage.setRotation(-correctedTilt);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Отримання швидкості в м/с та конвертація в км/г
        float speedMps = location.getSpeed();
        float speedKmh = speedMps * 3.6f;

        // Згладжування швидкості
        smoothedSpeed = speedAlpha * speedKmh + (1 - speedAlpha) * smoothedSpeed;

        // Отримання точності GPS
        float accuracy = location.getAccuracy();
        String accuracyText;
        if (accuracy <= 5) {
            accuracyText = "Відмінна";
        } else if (accuracy <= 10) {
            accuracyText = "Хороша";
        } else if (accuracy <= 20) {
            accuracyText = "Середня";
        } else {
            accuracyText = "Слабка";
        }

        // Оновлення відображення сирих GPS даних з точністю
        String gpsRawData = String.format("GPS: %.6f, %.6f, %.1fм, %.1fм/с, %s",
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude(),
                speedMps,
                accuracyText);
        rawGpsText.setText(gpsRawData);

        // Оновлення відображення швидкості з кольоровим кодуванням
        speedText.setText(String.format("%.1f км/г", smoothedSpeed));

        // Кольорове кодування тексту швидкості
        if (smoothedSpeed >= 100) {
            speedText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (smoothedSpeed >= 60) {
            speedText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else if (smoothedSpeed >= 30) {
            speedText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        } else {
            speedText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }

        // Оновлення стрілки спідометра (0 км/г = -90°, 180 км/г = +90°)
        // Мапування швидкості на кут: 0-180 км/г -> -90° до +90° (діапазон 180°)
        float speedAngle = (smoothedSpeed / 180f) * 180f;
        // Обмеження кута в межах шкали
        speedAngle = Math.max(-90f, Math.min(90f, speedAngle));
        speedArrowImage.setRotation(speedAngle);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Обробка змін статусу провайдера
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            startLocationUpdates();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            rawGpsText.setText("GPS: Вимкнено");
            speedText.setText("0 км/г");
            speedText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            speedArrowImage.setRotation(-90f); // Скидання до позиції 0 швидкості
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не використовується в даному випадку
    }
}