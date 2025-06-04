// файл: app/src/main/java/com/example/lab4/MediaFile.java
package com.example.lab4;
import android.net.Uri;

/**
 * Клас для представлення медіафайла
 * Містить всю необхідну інформацію про аудіо або відеофайл
 */
public class MediaFile {
    private String name;        // Ім'я файлу
    private String path;        // Шлях до файлу
    private Uri uri;           // URI файлу
    private long duration;     // Тривалість у мілісекундах
    private boolean isVideo;   // Прапорець типу файлу (true = відео, false = аудіо)

    public MediaFile(String name, String path, Uri uri, long duration, boolean isVideo) {
        this.name = name;
        this.path = path;
        this.uri = uri;
        this.duration = duration;
        this.isVideo = isVideo;
    }

    // Геттери для отримання даних файлу
    public String getName() { return name; }
    public String getPath() { return path; }
    public Uri getUri() { return uri; }
    public long getDuration() { return duration; }
    public boolean isVideo() { return isVideo; }

    /**
     * Повертає рядкове представлення об'єкта для відображення в списках
     * @return ім'я файлу
     */
    @Override
    public String toString() {
        return name;
    }
}