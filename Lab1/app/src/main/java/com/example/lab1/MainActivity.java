package com.example.lab1;

import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Spinner spinnerDishes;
    CheckBox man1, man2, man3, man4;
    NumberPicker minPricePicker, maxPricePicker;
    Button buttonOK;
    TextView textResult;

    // Масив назв страв
    String[] dishes = {
            "Оберіть страву",
            "Борщ",
            "Вареники ",
            "Голубці",
            "Салат",
            "Деруни",
            "Пампушки",
            "Сирники",
            "Стейк"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ініціалізація елементів інтерфейсу
        spinnerDishes = findViewById(R.id.spinner_dishes);
        man1 = findViewById(R.id.checkbox_manufacturer1);
        man2 = findViewById(R.id.checkbox_manufacturer2);
        man3 = findViewById(R.id.checkbox_manufacturer3);
        man4 = findViewById(R.id.checkbox_manufacturer4);
        minPricePicker = findViewById(R.id.number_min_price);
        maxPricePicker = findViewById(R.id.number_max_price);
        buttonOK = findViewById(R.id.button_ok);
        textResult = findViewById(R.id.text_result);

        // Заповнення Spinner-а стравами
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dishes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDishes.setAdapter(adapter);

        // Налаштування меж цін
        minPricePicker.setMinValue(0);
        minPricePicker.setMaxValue(100);
        minPricePicker.setValue(10);

        maxPricePicker.setMinValue(0);
        maxPricePicker.setMaxValue(100);
        maxPricePicker.setValue(30);

        // Обробка натискання кнопки
        buttonOK.setOnClickListener(v -> {
            int selectedIndex = spinnerDishes.getSelectedItemPosition();
            boolean anyManufacturer = man1.isChecked() || man2.isChecked() || man3.isChecked() || man4.isChecked();
            int minPrice = minPricePicker.getValue();
            int maxPrice = maxPricePicker.getValue();

            // Перевірка: страву не обрано
            if (selectedIndex == 0) {
                showError(getString(R.string.error_no_dish));
                return;
            }

            // Перевірка: не вибрано жодного виробника
            if (!anyManufacturer) {
                showError(getString(R.string.error_no_manufacturer));
                return;
            }

            // Перевірка: мінімальна ціна більша за максимальну
            if (minPrice > maxPrice) {
                showError(getString(R.string.error_price_range));
                return;
            }

            // Побудова рядка з результатом
            StringBuilder result = new StringBuilder();
            result.append("Страва: ").append(dishes[selectedIndex]).append("\n");

            result.append("Виробники: ");
            if (man1.isChecked()) result.append(getString(R.string.manufacturer1)).append(", ");
            if (man2.isChecked()) result.append(getString(R.string.manufacturer2)).append(", ");
            if (man3.isChecked()) result.append(getString(R.string.manufacturer3)).append(", ");
            if (man4.isChecked()) result.append(getString(R.string.manufacturer4)).append(", ");
            if (result.toString().endsWith(", "))
                result.setLength(result.length() - 2); // Видаляємо останню кому

            result.append("\nДіапазон цін: $").append(minPrice).append(" - $").append(maxPrice);

            // Вивід результату
            textResult.setText(result.toString());
        });
    }

    // Функція для показу діалогового вікна з помилкою
    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_title))
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
