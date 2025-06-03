package com.example.lab3;

import java.io.IOException;
import android.app.Activity;
import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class InputFragment extends Fragment {

    private Spinner spinnerDishes;
    private CheckBox man1, man2, man3, man4;
    private NumberPicker minPricePicker, maxPricePicker;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Ініціалізація елементів інтерфейсу
        spinnerDishes = view.findViewById(R.id.spinner_dishes);
        man1 = view.findViewById(R.id.checkbox_manufacturer1);
        man2 = view.findViewById(R.id.checkbox_manufacturer2);
        man3 = view.findViewById(R.id.checkbox_manufacturer3);
        man4 = view.findViewById(R.id.checkbox_manufacturer4);
        minPricePicker = view.findViewById(R.id.number_min_price);
        maxPricePicker = view.findViewById(R.id.number_max_price);
        Button buttonOK = view.findViewById(R.id.button_ok);

        // Заповнення спінера назвами страв
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, dishes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDishes.setAdapter(adapter);

        // Налаштування меж цін
        minPricePicker.setMinValue(0);
        minPricePicker.setMaxValue(100);
        minPricePicker.setValue(10);

        maxPricePicker.setMinValue(0);
        maxPricePicker.setMaxValue(100);
        maxPricePicker.setValue(30);

        // Кнопка відкриття історії
        Button buttonOpen = view.findViewById(R.id.button_open);
        buttonOpen.setOnClickListener(v -> {
            // Запуск HistoryActivity для отримання результату
            startActivityForResult(new Intent(requireContext(), HistoryActivity.class), 1234);
        });

        // Обробка натискання кнопки "ОК" (збереження вибору)
        buttonOK.setOnClickListener(v -> {
            int selectedIndex = spinnerDishes.getSelectedItemPosition();
            boolean anyManufacturer = man1.isChecked() || man2.isChecked() || man3.isChecked() || man4.isChecked();
            int minPrice = minPricePicker.getValue();
            int maxPrice = maxPricePicker.getValue();

            // Перевірка: страву не обрано
            if (selectedIndex == 0) {
                showError("Будь ласка, оберіть страву.");
                return;
            }

            // Перевірка: не вибрано жодного виробника
            if (!anyManufacturer) {
                showError("Будь ласка, оберіть хоча б одного виробника.");
                return;
            }

            // Перевірка: мінімальна ціна більша за максимальну
            if (minPrice > maxPrice) {
                showError("Мінімальна ціна не може бути більшою за максимальну.");
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
                result.setLength(result.length() - 2);

            result.append("\nДіапазон цін: ₴").append(minPrice).append(" - ₴").append(maxPrice);

            // Збереження вибору у файл
            try {
                SelectionData selection = new SelectionData(
                        dishes[selectedIndex],
                        result.toString().split("\n")[1].replace("Виробники: ", ""),
                        minPrice,
                        maxPrice
                );
                StorageWorker.save(requireContext(), selection);
                Toast.makeText(requireContext(), "Запис збережено успішно", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Помилка збереження: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            // Створення та передача результату до ResultFragment
            ResultFragment resultFragment = ResultFragment.newInstance(result.toString());

            // Додавання фрагмента результату
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.result_fragment_container, resultFragment)
                    .commit();

            // Показ контейнера з результатом і роздільної лінії
            requireActivity().findViewById(R.id.result_fragment_container).setVisibility(View.VISIBLE);
            requireActivity().findViewById(R.id.separator_line).setVisibility(View.VISIBLE);
        });
    }

    // Обробка повернення результату із HistoryActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234 && resultCode == Activity.RESULT_OK && data != null) {
            String dish = data.getStringExtra(HistoryActivity.EXTRA_DISH);
            String manufacturers = data.getStringExtra(HistoryActivity.EXTRA_MANUFACTURERS);
            int minPrice = data.getIntExtra(HistoryActivity.EXTRA_MIN_PRICE, 0);
            int maxPrice = data.getIntExtra(HistoryActivity.EXTRA_MAX_PRICE, 0);

            setFormFromSelection(new SelectionData(dish, manufacturers, minPrice, maxPrice));
        }
    }

    // Встановити дані форми з обраного SelectionData
    public void setFormFromSelection(SelectionData selection) {
        // Встановлення страви
        for (int i = 0; i < dishes.length; i++) {
            if (dishes[i].equals(selection.dish)) {
                spinnerDishes.setSelection(i);
                break;
            }
        }
        // Встановлення виробників
        man1.setChecked(selection.manufacturers.contains(getString(R.string.manufacturer1)));
        man2.setChecked(selection.manufacturers.contains(getString(R.string.manufacturer2)));
        man3.setChecked(selection.manufacturers.contains(getString(R.string.manufacturer3)));
        man4.setChecked(selection.manufacturers.contains(getString(R.string.manufacturer4)));

        // Встановлення цін
        minPricePicker.setValue(selection.minPrice);
        maxPricePicker.setValue(selection.maxPrice);
    }

    // Функція для показу діалогового вікна з помилкою
    private void showError(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Помилка")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Очищення форми
    public void resetForm() {
        spinnerDishes.setSelection(0);
        man1.setChecked(false);
        man2.setChecked(false);
        man3.setChecked(false);
        man4.setChecked(false);
        minPricePicker.setValue(10);
        maxPricePicker.setValue(30);
    }
}
