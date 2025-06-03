package com.example.lab3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

// Активність для перегляду історії збережених виборів користувача
public class HistoryActivity extends AppCompatActivity {
    private ArrayAdapter<String> adapter;
    private List<SelectionData> savedItems;

    // Ключі для передачі даних назад у поля вводу
    public static final String EXTRA_DISH = "extra_dish";
    public static final String EXTRA_MANUFACTURERS = "extra_manufacturers";
    public static final String EXTRA_MIN_PRICE = "extra_min_price";
    public static final String EXTRA_MAX_PRICE = "extra_max_price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Список для відображення пам'яті
        ListView listView = findViewById(R.id.listViewHistory);

        try {
            savedItems = StorageWorker.load(this); // Завантаження збережених елементів
            if (savedItems.isEmpty()) {
                showMessage("Немає збережених записів.");
                return;
            }

            // Заповнюємо адаптер текстовими представленнями виборів
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            for (SelectionData item : savedItems)
                adapter.add(item.toString());
            listView.setAdapter(adapter);

            // Повертаємо вибраний запис до InputFragment
            listView.setOnItemClickListener((parent, view1, position, id) -> {
                SelectionData selected = savedItems.get(position);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_DISH, selected.dish);
                resultIntent.putExtra(EXTRA_MANUFACTURERS, selected.manufacturers);
                resultIntent.putExtra(EXTRA_MIN_PRICE, selected.minPrice);
                resultIntent.putExtra(EXTRA_MAX_PRICE, selected.maxPrice);
                setResult(RESULT_OK, resultIntent);
                finish();
            });

            // Довге натискання: видалити запис
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Видалити запис?")
                        .setMessage("Бажаєте видалити цей запис?")
                        .setPositiveButton("Так", (dialog, which) -> {
                            savedItems.remove(position);
                            updateStorage();
                        })
                        .setNegativeButton("Ні", null)
                        .show();
                return true;
            });

        } catch (IOException e) {
            showMessage("Помилка при зчитуванні: " + e.getMessage());
        }

        // Обробка кнопки "Назад"
        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());
    }

    // Оновлення збережених даних після видалення
    private void updateStorage() {
        StorageWorker.clear(this);
        try {
            for (SelectionData item : savedItems) {
                StorageWorker.save(this, item);
            }
            adapter.clear();
            for (SelectionData item : savedItems)
                adapter.add(item.toString());
        } catch (IOException e) {
            showMessage("Помилка оновлення: " + e.getMessage());
        }
    }

    // Відображення інформаційного вікна
    private void showMessage(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Інформація")
                .setMessage(msg)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }
}
