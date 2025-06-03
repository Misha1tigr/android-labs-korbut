package com.example.lab3;

import androidx.annotation.NonNull;

// Клас для зберігання вибору страви користувача
public class SelectionData {
    public String dish;
    public String manufacturers;
    public int minPrice;
    public int maxPrice;

    public SelectionData(String dish, String manufacturers, int minPrice, int maxPrice) {
        this.dish = dish;
        this.manufacturers = manufacturers;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    // Перетворення об'єкта у рядок
    @NonNull
    @Override
    public String toString() {
        return "Страва: " + dish + "\nВиробники: " + manufacturers + "\nДіапазон цін: ₴" + minPrice + " - ₴" + maxPrice;
    }
}
