package com.biim3210.mediaplayer.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

import java.util.function.DoubleConsumer;

public class VolumeControls {

    private final Button muteBtn = new Button("Mute");
    private final Slider slider = new Slider(0, 100, 50);
    private final Label label = new Label("Volume: 50%");
    private final HBox container;

    public VolumeControls(Runnable onMute, DoubleConsumer onVolumeChange) {
        slider.setPrefWidth(200);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double v = newVal.doubleValue();
            label.setText("Volume: " + (int) v + "%");
            onVolumeChange.accept(v);
        });

        muteBtn.setOnAction(e -> onMute.run());

        container = new HBox(10, muteBtn, slider, label);
        container.setAlignment(Pos.CENTER);
    }

    public HBox getView() {
        return container;
    }

    public double getVolume() {
        return slider.getValue();
    }

    public void setVolume(double v) {
        slider.setValue(v);
    }

    public void setMuted(boolean muted) {
        muteBtn.setText(muted ? "Unmute" : "Mute");
    }
}