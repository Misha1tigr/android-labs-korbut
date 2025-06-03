package com.example.lab2;

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
        // Інфляція макету
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

        // Обробка натискання кнопки "ОК"
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

    // Функція для показу діалогового вікна з помилкою
    private void showError(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Помилка")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // очищення форми (використовується у ResultFragment)
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
