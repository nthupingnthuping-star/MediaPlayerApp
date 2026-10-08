package com.biim3210.mediaplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerApp extends Application {

    // ----- VLCJ Components -----
    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;
    private ImageView videoImageView;

    // ----- UI Components -----
    private ListView<String> playlistView;
    private final List<File> playlistFiles = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isPlaying = false;

    private Label statusLabel;
    private Button playPauseBtn, stopBtn, nextBtn, prevBtn, muteBtn;
    private Button addBtn, removeBtn;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Label volumeLabel;
    private Label timeLabel;

    // ----- Time updater thread -----
    private Thread timeUpdaterThread;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize VLCJ
        mediaPlayerFactory = new MediaPlayerFactory();
        mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        // 2. Setup the Video Surface (ImageView)
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.setFitWidth(720);
        videoImageView.setFitHeight(400);

        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(this.videoImageView));

        // 3. Build UI
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Center: Video area
        StackPane mediaPane = new StackPane(videoImageView);
        mediaPane.getStyleClass().add("media-pane");
        mediaPane.setMinSize(720, 400);

        // Top: Status
        statusLabel = new Label("Ready. Add files to start.");
        statusLabel.getStyleClass().add("status-label");

        // Bottom: Controls
        VBox bottomBox = new VBox(12);
        bottomBox.setPadding(new Insets(15, 0, 0, 0));

        // ---- Progress bar with time label ----
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setDisable(true);
        progressSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null) {
                long total = mediaPlayer.status().length();
                if (total > 0) {
                    long seekTo = (long) (total * progressSlider.getValue() / 100.0);
                    mediaPlayer.controls().setTime(seekTo);
                }
            }
        });

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setMinWidth(120);
        timeLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #1c1c1e;");

        Label progressLabel = new Label("Progress:");
        progressLabel.getStyleClass().add("section-label");

        HBox progressBox = new HBox(10, progressLabel, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        // ---- Playback buttons ----
        playPauseBtn = new Button("Play");
        stopBtn = new Button("Stop");
        prevBtn = new Button("Previous");
        nextBtn = new Button("Next");

        playPauseBtn.getStyleClass().add("button-primary");
        stopBtn.getStyleClass().add("button-danger");

        playPauseBtn.setOnAction(e -> togglePlayPause());
        stopBtn.setOnAction(e -> stopPlayback());
        prevBtn.setOnAction(e -> playPrevious());
        nextBtn.setOnAction(e -> playNext());

        HBox playbackBox = new HBox(12, prevBtn, playPauseBtn, stopBtn, nextBtn);
        playbackBox.setAlignment(Pos.CENTER);

        // ---- Volume controls ----
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(220);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.audio().setVolume(newVal.intValue() * 2);
            }
            volumeLabel.setText("Volume: " + (int) newVal.doubleValue() + "%");
        });

        volumeLabel = new Label("Volume: 50%");
        muteBtn = new Button("Mute");
        muteBtn.setOnAction(e -> toggleMute());

        HBox volumeBox = new HBox(12, muteBtn, volumeSlider, volumeLabel);
        volumeBox.setAlignment(Pos.CENTER);

        // ---- Playlist buttons ----
        addBtn = new Button("Add Files");
        removeBtn = new Button("Remove Selected");
        addBtn.getStyleClass().add("button-success");
        addBtn.setOnAction(e -> addFiles(primaryStage));
        removeBtn.setOnAction(e -> removeSelected());

        HBox playlistBtnBox = new HBox(12, addBtn, removeBtn);
        playlistBtnBox.setAlignment(Pos.CENTER);

        bottomBox.getChildren().addAll(
                progressBox, playbackBox, volumeBox, playlistBtnBox
        );

        // Right: Playlist
        playlistView = new ListView<>();
        playlistView.setPrefWidth(320);
        playlistView.setPlaceholder(new Label("Playlist is empty"));

        playlistView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldVal, newVal) -> {
                    int idx = newVal.intValue();
                    if (idx >= 0 && idx < playlistFiles.size() && idx != currentIndex) {
                        currentIndex = idx;
                        loadAndPlay(playlistFiles.get(idx));
                    }
                });

        Label playlistTitle = new Label("Playlist:");
        playlistTitle.getStyleClass().add("section-label");

        VBox playlistBox = new VBox(8, playlistTitle, playlistView);
        playlistBox.setPadding(new Insets(0, 0, 0, 15));
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        // Assemble
        root.setTop(statusLabel);
        root.setCenter(mediaPane);
        root.setBottom(bottomBox);
        root.setRight(playlistBox);

        // Scene + CSS
        Scene scene = new Scene(root, 1150, 720);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles.");
        }

        // Keyboard controls
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case SPACE:  togglePlayPause(); event.consume(); break;
                case S:      stopPlayback();    event.consume(); break;
                case N:      playNext();        event.consume(); break;
                case P:      playPrevious();    event.consume(); break;
                case M:      toggleMute();      event.consume(); break;
                case UP:     volumeSlider.setValue(volumeSlider.getValue() + 5); event.consume(); break;
                case DOWN:   volumeSlider.setValue(volumeSlider.getValue() - 5); event.consume(); break;
                default: break;
            }
        });

        primaryStage.setTitle("Media Player");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.controls().stop();
                mediaPlayer.release();
            }
            if (mediaPlayerFactory != null) {
                mediaPlayerFactory.release();
            }
            Platform.exit();
        });

        // Start the time updater thread
        startTimeUpdater();

        primaryStage.show();
    }

    // ================= TIME UPDATER =================

    private void startTimeUpdater() {
        timeUpdaterThread = new Thread(() -> {
            while (true) {
                try {
                    if (mediaPlayer != null && mediaPlayer.status().isPlaying()) {
                        final long current = mediaPlayer.status().time();
                        final long total = mediaPlayer.status().length();

                        final String currentStr = formatTime(current);
                        final String totalStr = formatTime(total);

                        Platform.runLater(() -> {
                            timeLabel.setText(currentStr + " / " + totalStr);
                            if (total > 0) {
                                progressSlider.setValue(
                                        (double) current / total * 100.0
                                );
                                progressSlider.setDisable(false);
                            }
                        });
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeUpdaterThread.setDaemon(true);
        timeUpdaterThread.start();
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "00:00";
        long totalSec = millis / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    // ================= PLAYBACK =================

    private void loadAndPlay(File file) {
        if (mediaPlayer == null) return;

        if (timeLabel != null) timeLabel.setText("00:00 / 00:00");
        progressSlider.setValue(0);

        mediaPlayer.media().play(file.getAbsolutePath());

        isPlaying = true;
        statusLabel.setText("Now Playing: " + file.getName());
        playPauseBtn.setText("Pause");
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.controls().pause();
            statusLabel.setText("Paused");
        } else {
            mediaPlayer.controls().play();
            statusLabel.setText("Playing");
        }
        isPlaying = !isPlaying;
        playPauseBtn.setText(isPlaying ? "Pause" : "Play");
    }

    private void stopPlayback() {
        if (mediaPlayer == null) return;
        mediaPlayer.controls().stop();
        isPlaying = false;
        playPauseBtn.setText("Play");
        statusLabel.setText("Stopped");
        if (timeLabel != null) timeLabel.setText("00:00 / 00:00");
        progressSlider.setValue(0);
    }

    private void playNext() {
        if (playlistFiles.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlistFiles.size();
        loadAndPlay(playlistFiles.get(currentIndex));
        playlistView.getSelectionModel().select(currentIndex);
    }

    private void playPrevious() {
        if (playlistFiles.isEmpty()) return;
        currentIndex = (currentIndex - 1 + playlistFiles.size()) % playlistFiles.size();
        loadAndPlay(playlistFiles.get(currentIndex));
        playlistView.getSelectionModel().select(currentIndex);
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        boolean muted = mediaPlayer.audio().isMute();
        mediaPlayer.audio().setMute(!muted);
        muteBtn.setText(muted ? "Mute" : "Unmute");
        statusLabel.setText(muted ? "Unmuted" : "Muted");
    }

    // ================= PLAYLIST =================

    private void addFiles(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Media", "*.*"),
                new FileChooser.ExtensionFilter("Video",
                        "*.mp4", "*.mkv", "*.avi", "*.mov", "*.webm", "*.flv"),
                new FileChooser.ExtensionFilter("Audio",
                        "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac", "*.mpeg")
        );

        List<File> selected = fileChooser.showOpenMultipleDialog(stage);
        if (selected != null && !selected.isEmpty()) {
            for (File f : selected) {
                playlistFiles.add(f);
                playlistView.getItems().add(f.getName());
            }
            statusLabel.setText(selected.size() + " file(s) added.");

            if (mediaPlayer != null && currentIndex == -1) {
                currentIndex = 0;
                loadAndPlay(playlistFiles.get(0));
            }
        }
    }

    private void removeSelected() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            statusLabel.setText("Select an item to remove.");
            return;
        }

        playlistFiles.remove(idx);
        playlistView.getItems().remove(idx);

        if (idx == currentIndex) {
            stopPlayback();
            if (playlistFiles.isEmpty()) {
                currentIndex = -1;
                videoImageView.setImage(null);
                statusLabel.setText("Playlist is empty.");
            } else {
                currentIndex = Math.min(idx, playlistFiles.size() - 1);
                loadAndPlay(playlistFiles.get(currentIndex));
            }
        } else if (idx < currentIndex) {
            currentIndex--;
        }
        statusLabel.setText("Removed selected item.");
    }
}