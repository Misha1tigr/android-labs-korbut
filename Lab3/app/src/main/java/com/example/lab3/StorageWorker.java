package com.example.lab3;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Допоміжний клас для збереження і читання виборів користувача у файл
public class StorageWorker {
    private static final String FILE_NAME = "dish_selections.txt"; // Ім'я файлу для зберігання

    // Зберегти один вибір у файл
    public static void save(Context context, SelectionData selection) throws IOException {
        try (FileWriter fw = new FileWriter(new File(context.getFilesDir(), FILE_NAME), true)) {
            fw.write(selection.dish + ";" + selection.manufacturers + ";" + selection.minPrice + ";" + selection.maxPrice + "\n");
        }
    }

    // Завантажити всі збережені вибори з файлу
    public static List<SelectionData> load(Context context) throws IOException {
        List<SelectionData> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list; // Якщо файл не існує — повертаємо порожній список

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 4) {
                    list.add(new SelectionData(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                }
            }
        }
        return list;
    }

    // Очистити всі збережені вибори (видалити файл)
    public static void clear(Context context) {
        new File(context.getFilesDir(), FILE_NAME).delete();
    }
}
