package com.example.lab4;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Константи для запитів дозволів та вибору файлів
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_FILE_REQUEST = 200;

    // Компоненти інтерфейсу користувача
    private Spinner mediaTypeSpinner; // Випадаючий список типів медіа
    private ListView fileListView; // Список файлів
    private Button selectFileButton; // Кнопка вибору файлу

    private boolean isVideoMode = false; // Прапорець режиму відео
    private List<MediaFile> audioFiles = new ArrayList<>(); // Список аудіофайлів
    private List<MediaFile> videoFiles = new ArrayList<>(); // Список відеофайлів
    private ArrayAdapter<MediaFile> fileAdapter; // Адаптер для списку файлів

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide(); // Приховати панель дій
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupSpinner();
        setupListeners();
        checkPermissions();
    }

    /**
     * Ініціалізація елементів інтерфейсу користувача
     */
    private void initializeViews() {
        mediaTypeSpinner = findViewById(R.id.mediaTypeSpinner);
        fileListView = findViewById(R.id.fileListView);
        selectFileButton = findViewById(R.id.selectFileButton);
    }

    /**
     * Налаштування випадаючого списку типів медіа
     */
    private void setupSpinner() {
        String[] mediaTypes = {"Аудіофайли", "Відеофайли"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, mediaTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mediaTypeSpinner.setAdapter(spinnerAdapter);
    }

    /**
     * Налаштування слухачів подій для елементів інтерфейсу
     */
    private void setupListeners() {
        mediaTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isVideoMode = (position == 1); // Встановити режим відео якщо вибрано другий елемент
                updateFileList(); // Оновити список файлів
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Слухач списку файлів
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaFile selectedFile = fileAdapter.getItem(position);
                if (selectedFile != null) {
                    launchPlayer(selectedFile); // Запустити плеєр з вибраним файлом
                }
            }
        });

        // Слухач кнопки вибору файлу
        selectFileButton.setOnClickListener(v -> openFilePicker());
    }

    /**
     * Перевірка та запит необхідних дозволів
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // Використання нових детальних дозволів для медіа
            List<String> permissionsNeeded = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }

            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissionsNeeded.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMediaFiles(); // Завантажити медіафайли якщо дозволи надані
            }
        } else {
            // Використання застарілого дозволу для старіших версій Android
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMediaFiles(); // Завантажити медіафайли якщо дозволи надані
            }
        }
    }

    /**
     * Обробка результату запиту дозволів
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            // Перевірити чи всі дозволи надані
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                loadMediaFiles(); // Завантажити медіафайли якщо всі дозволи надані
            } else {
                Toast.makeText(this, "Дозволи на доступ до медіафайлів необхідні для роботи",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Перевірка наявності дозволу на читання аудіофайлів
     */
    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Перевірка наявності дозволу на читання відеофайлів
     */
    private boolean hasVideoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Завантаження всіх медіафайлів відповідно до наданих дозволів
     */
    private void loadMediaFiles() {
        if (hasAudioPermission()) {
            loadAudioFiles(); // Завантажити аудіофайли
        }
        if (hasVideoPermission()) {
            loadVideoFiles(); // Завантажити відеофайли
        }
        updateFileList(); // Оновити відображення списку файлів
    }

    /**
     * Завантаження аудіофайлів з медіасховища
     */
    private void loadAudioFiles() {
        audioFiles.clear(); // Очистити попередній список

        // Визначити колонки для запиту
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                MediaStore.Audio.Media.DISPLAY_NAME + " ASC")) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                // Обробити кожен запис
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(pathColumn);
                    long duration = cursor.getLong(durationColumn);

                    // Створити URI для файлу
                    Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id));

                    // Додати файл до списку
                    audioFiles.add(new MediaFile(name, path, uri, duration, false));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Помилка завантаження аудіофайлів: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Завантаження відеофайлів з медіасховища
     */
    private void loadVideoFiles() {
        videoFiles.clear(); // Очистити попередній список

        // Визначити колонки для запиту
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                MediaStore.Video.Media.DISPLAY_NAME + " ASC")) {

            if (cursor != null) {
                // Отримати індекси колонок
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                // Обробити кожен запис
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(pathColumn);
                    long duration = cursor.getLong(durationColumn);

                    // Створити URI для файлу
                    Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id));

                    // Додати файл до списку
                    videoFiles.add(new MediaFile(name, path, uri, duration, true));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Помилка завантаження відеофайлів: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Оновлення відображення списку файлів відповідно до поточного режиму
     */
    private void updateFileList() {
        List<MediaFile> currentFiles = isVideoMode ? videoFiles : audioFiles;
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currentFiles);
        fileListView.setAdapter(fileAdapter);
    }

    /**
     * Відкриття вікна вибору файлу
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        // Встановити тип файлів відповідно до режиму
        if (isVideoMode) {
            intent.setType("video/*");
        } else {
            intent.setType("audio/*");
        }

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Виберіть медіафайл"), PICK_FILE_REQUEST);
    }

    /**
     * Обробка результату вибору файлу
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();

            if (selectedFileUri != null) {
                String fileName = getFileName(selectedFileUri); // Отримати ім'я файлу
                MediaFile selectedFile = new MediaFile(fileName, "", selectedFileUri, 0, isVideoMode);
                launchPlayer(selectedFile); // Запустити плеєр з вибраним файлом
            }
        }
    }

    /**
     * Отримання імені файлу з URI
     */
    private String getFileName(Uri uri) {
        String result = null;

        // Спробувати отримати ім'я через ContentResolver
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }

        // Якщо не вдалося отримати ім'я, витягти з шляху
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    /**
     * Запуск плеєра з вибраним медіафайлом
     */
    private void launchPlayer(MediaFile mediaFile) {
        Intent intent = new Intent(this, PlayerActivity.class);

        // Передати дані про файл
        intent.putExtra("file_name", mediaFile.getName());
        intent.putExtra("file_uri", mediaFile.getUri().toString());
        intent.putExtra("is_video", mediaFile.isVideo());
        intent.putExtra("duration", mediaFile.getDuration());

        startActivity(intent);
    }
}