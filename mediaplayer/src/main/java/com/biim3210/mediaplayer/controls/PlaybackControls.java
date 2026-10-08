package com.biim3210.mediaplayer.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class PlaybackControls {

    private final Button playPauseBtn = new Button("Play");
    private final Button stopBtn = new Button("Stop");
    private final Button prevBtn = new Button("Previous");
    private final Button nextBtn = new Button("Next");
    private final HBox container;

    public PlaybackControls(Runnable onPlayPause, Runnable onStop,
            Runnable onPrev, Runnable onNext) {
        playPauseBtn.setOnAction(e -> onPlayPause.run());
        stopBtn.setOnAction(e -> onStop.run());
        prevBtn.setOnAction(e -> onPrev.run());
        nextBtn.setOnAction(e -> onNext.run());

        container = new HBox(10, prevBtn, playPauseBtn, stopBtn, nextBtn);
        container.setAlignment(Pos.CENTER);
    }

    public HBox getView() {
        return container;
    }

    public void setPlaying(boolean playing) {
        playPauseBtn.setText(playing ? "Pause" : "Play");
    }
}