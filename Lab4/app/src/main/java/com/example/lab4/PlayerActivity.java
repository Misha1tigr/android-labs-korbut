package com.example.lab4;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Активність програвача мультимедійних файлів
 * Підтримує відтворення відео та аудіо файлів з метаданими
 */
public class PlayerActivity extends AppCompatActivity {

    // Компоненти інтерфейсу користувача
    private VideoView videoView;
    private RelativeLayout controlsOverlay;
    private Button playPauseButton, stopButton, backButton;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;

    private ImageView albumArtImageView;
    private TextView artistText, albumText, titleText, genreText, yearText, bitrateText;

    // Медіа компоненти
    private MediaPlayer audioPlayer;
    private final Handler handler = new Handler();
    private final Handler hideControlsHandler = new Handler();

    // Змінні стану
    private boolean isPlaying = false;
    private boolean isVideoMode = false;
    private boolean controlsVisible = true;
    private String fileName;
    private Uri fileUri;
    private boolean shouldAutoStart = true;

    // Детектор жестів для показу/приховання елементів керування
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Отримання даних з intent
        fileName = getIntent().getStringExtra("file_name");
        String uriString = getIntent().getStringExtra("file_uri");
        isVideoMode = getIntent().getBooleanExtra("is_video", false);

        if (uriString != null) {
            fileUri = Uri.parse(uriString);
        }

        // Встановлення орієнтації для відео
        if (isVideoMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_player);
        initializeViews();
        setupGestureDetector();
        setupListeners();
        loadMedia();
    }

    /**
     * Ініціалізація всіх компонентів інтерфейсу
     */
    private void initializeViews() {
        videoView = findViewById(R.id.videoView);
        controlsOverlay = findViewById(R.id.controlsOverlay);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);
        backButton = findViewById(R.id.backButton);
        seekBar = findViewById(R.id.seekBar);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        TextView fileNameText = findViewById(R.id.fileNameText);

        // Компоненти метаданих аудіо
        // Компоненти інтерфейсу для метаданих аудіо
        LinearLayout audioMetadataContainer = findViewById(R.id.audioMetadataContainer);
        albumArtImageView = findViewById(R.id.albumArtImageView);
        artistText = findViewById(R.id.artistText);
        albumText = findViewById(R.id.albumText);
        titleText = findViewById(R.id.titleText);
        genreText = findViewById(R.id.genreText);
        yearText = findViewById(R.id.yearText);
        bitrateText = findViewById(R.id.bitrateText);

        fileNameText.setText(fileName);

        // Показ/приховання компонентів залежно від типу медіа
        if (!isVideoMode) {
            audioPlayer = new MediaPlayer();
            videoView.setVisibility(View.GONE);
            audioMetadataContainer.setVisibility(View.VISIBLE);
        } else {
            videoView.setVisibility(View.VISIBLE);
            audioMetadataContainer.setVisibility(View.GONE);
        }

        // Встановлення початкового стану кнопки
        playPauseButton.setText("▶");
    }

    /**
     * Налаштування детектора жестів для керування відображенням елементів
     */
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControlsVisibility();
                return true;
            }
        });
    }

    /**
     * Налаштування слухачів подій для всіх інтерактивних елементів
     */
    private void setupListeners() {
        // Кнопка відтворення/паузи
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        // Кнопка зупинки
        stopButton.setOnClickListener(v -> stopMedia());

        // Кнопка повернення
        backButton.setOnClickListener(v -> finish());

        // Слухач для seekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isVideoMode && videoView.canSeekForward()) {
                        videoView.seekTo(progress);
                    } else if (!isVideoMode && audioPlayer != null) {
                        audioPlayer.seekTo(progress);
                    }
                    currentTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                showControls();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleHideControls();
            }
        });

        // Слухач дотику для детекції жестів (тільки для відео)
        if (isVideoMode) {
            videoView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }

    /**
     * Завантаження медіафайлу залежно від типу
     */
    private void loadMedia() {
        if (isVideoMode) {
            loadVideo();
        } else {
            loadAudio();
            loadAudioMetadata();
        }
    }

    /**
     * Завантаження та налаштування відео
     */
    private void loadVideo() {
        try {
            videoView.setVideoURI(fileUri);
            videoView.setOnPreparedListener(mp -> {
                int duration = videoView.getDuration();
                seekBar.setMax(duration);
                totalTimeText.setText(formatTime(duration));

                videoView.start();
                isPlaying = true;
                playPauseButton.setText("⏸");
                updateSeekBar();
                scheduleHideControls();
            });

            videoView.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setText("▶");
                seekBar.setProgress(0);
                currentTimeText.setText("00:00");
                showControls();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Помилка відтворення відео", Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (Exception e) {
            Toast.makeText(this, "Помилка завантаження відео: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Завантаження та налаштування аудіо
     */
    private void loadAudio() {
        try {
            audioPlayer.setDataSource(this, fileUri);
            audioPlayer.prepareAsync();

            audioPlayer.setOnPreparedListener(mp -> {
                int duration = audioPlayer.getDuration();
                seekBar.setMax(duration);
                totalTimeText.setText(formatTime(duration));

                if (shouldAutoStart) {
                    audioPlayer.start();
                    isPlaying = true;
                    playPauseButton.setText("⏸");
                    updateSeekBar();
                } else {
                    isPlaying = false;
                    playPauseButton.setText("▶");
                    seekBar.setProgress(0);
                    currentTimeText.setText("00:00");
                }
            });

            audioPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setText("▶");
                seekBar.setProgress(0);
                currentTimeText.setText("00:00");
            });

            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Помилка відтворення аудіо", Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (IOException e) {
            Toast.makeText(this, "Помилка завантаження аудіо: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Завантаження метаданих аудіофайлу та обкладинки альбому
     */
    private void loadAudioMetadata() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, fileUri);

            // Вилучення метаданих
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);

            // Вилучення обкладинки альбому
            byte[] albumArtBytes = retriever.getEmbeddedPicture();

            // Оновлення інтерфейсу в головному потоці
            runOnUiThread(() -> {
                // Встановлення тексту метаданих
                artistText.setText("Виконавець: " + (artist != null ? artist : "Невідомо"));
                albumText.setText("Альбом: " + (album != null ? album : "Невідомо"));
                titleText.setText("Назва: " + (title != null ? title : fileName));
                genreText.setText("Жанр: " + (genre != null ? genre : "Невідомо"));
                yearText.setText("Рік: " + (year != null ? year : "Невідомо"));

                if (bitrate != null) {
                    int bitrateKbps = Integer.parseInt(bitrate) / 1000;
                    bitrateText.setText("Бітрейт: " + bitrateKbps + " кбіт/с");
                } else {
                    bitrateText.setText("Бітрейт: Невідомо");
                }

                // Встановлення обкладинки альбому
                if (albumArtBytes != null) {
                    Bitmap albumArt = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.length);
                    albumArtImageView.setImageBitmap(albumArt);
                    albumArtImageView.setVisibility(View.VISIBLE);
                } else {
                    // Встановлення стандартної обкладинки
                    albumArtImageView.setImageResource(android.R.drawable.ic_media_play);
                    albumArtImageView.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            // Якщо витягування метаданих не вдалося, показати базову інформацію
            runOnUiThread(() -> {
                artistText.setText("Виконавець: Невідомо");
                albumText.setText("Альбом: Невідомо");
                titleText.setText("Назва: " + fileName);
                genreText.setText("Жанр: Невідомо");
                yearText.setText("Рік: Невідомо");
                bitrateText.setText("Бітрейт: Невідомо");
                albumArtImageView.setImageResource(android.R.drawable.ic_media_play);
                albumArtImageView.setVisibility(View.VISIBLE);
            });
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                // Ігнорувати помилки при звільненні
            }
        }
    }

    /**
     * Переключення між відтворенням та паузою
     */
    private void togglePlayPause() {
        if (isPlaying) {
            pauseMedia();
        } else {
            playMedia();
        }
        showControls();
        scheduleHideControls();
    }

    /**
     * Запуск медіафайлу
     */
    private void playMedia() {
        shouldAutoStart = true;
        if (isVideoMode && !videoView.isPlaying()) {
            videoView.start();
            isPlaying = true;
            playPauseButton.setText("⏸");
            updateSeekBar();
        } else if (!isVideoMode && audioPlayer != null && !audioPlayer.isPlaying()) {
            audioPlayer.start();
            isPlaying = true;
            playPauseButton.setText("⏸");
            updateSeekBar();
        }
    }

    /**
     * Призупинення медіафайлу
     */
    private void pauseMedia() {
        if (isVideoMode && videoView.isPlaying()) {
            videoView.pause();
            isPlaying = false;
            playPauseButton.setText("▶");
        } else if (!isVideoMode && audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            isPlaying = false;
            playPauseButton.setText("▶");
        }
    }

    /**
     * Зупинка медіафайлу та скидання до початку
     */
    private void stopMedia() {
        isPlaying = false;
        playPauseButton.setText("▶");

        if (isVideoMode) {
            videoView.pause();
            videoView.seekTo(0);
        } else if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stop();
            }
            try {
                shouldAutoStart = false; // запобігання автозапуску після підготовки
                audioPlayer.reset();
                audioPlayer.setDataSource(this, fileUri);
                audioPlayer.prepare();
            } catch (IOException e) {
                Toast.makeText(this, "Помилка скидання аудіо", Toast.LENGTH_SHORT).show();
            }
        }

        seekBar.setProgress(0);
        currentTimeText.setText("00:00");
        handler.removeCallbacks(updateSeekBarRunnable);
        showControls();
    }

    /**
     * Запуск оновлення прогрес-бару
     */
    private void updateSeekBar() {
        handler.post(updateSeekBarRunnable);
    }

    /**
     * Runnable для регулярного оновлення прогрес-бару
     */
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                int currentPosition = 0;
                if (isVideoMode && videoView.isPlaying()) {
                    currentPosition = videoView.getCurrentPosition();
                } else if (!isVideoMode && audioPlayer != null && audioPlayer.isPlaying()) {
                    currentPosition = audioPlayer.getCurrentPosition();
                }
                seekBar.setProgress(currentPosition);
                currentTimeText.setText(formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Переключення видимості елементів керування
     */
    private void toggleControlsVisibility() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
            scheduleHideControls();
        }
    }

    /**
     * Показ елементів керування
     */
    private void showControls() {
        controlsOverlay.setVisibility(View.VISIBLE);
        controlsVisible = true;
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
    }

    /**
     * Приховання елементів керування
     */
    private void hideControls() {
        if (isVideoMode && isPlaying) {
            controlsOverlay.setVisibility(View.GONE);
            controlsVisible = false;
        }
    }

    /**
     * Планування автоматичного приховання елементів керування
     */
    private void scheduleHideControls() {
        if (isVideoMode && isPlaying) {
            hideControlsHandler.removeCallbacks(hideControlsRunnable);
            hideControlsHandler.postDelayed(hideControlsRunnable, 5000); // Збільшено до 5 секунд для прокрутки
        }
    }

    /**
     * Runnable для приховання елементів керування
     */
    private Runnable hideControlsRunnable = this::hideControls;

    /**
     * Форматування часу з мілісекунд у MM:SS формат
     */
    private String formatTime(int milliseconds) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMedia();
    }

    /**
     * Очищення ресурсів при знищенні активності
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null) {
            audioPlayer.release();
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
    }

    /**
     * Обробка зміни конфігурації (поворот екрану)
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}