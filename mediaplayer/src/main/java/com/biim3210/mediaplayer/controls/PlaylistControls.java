package com.biim3210.mediaplayer.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class PlaylistControls {

    private final HBox container;

    public PlaylistControls(Runnable onAdd, Runnable onRemove) {
        Button addBtn = new Button("Add Files");
        Button removeBtn = new Button("Remove Selected");

        addBtn.setOnAction(e -> onAdd.run());
        removeBtn.setOnAction(e -> onRemove.run());

        container = new HBox(10, addBtn, removeBtn);
        container.setAlignment(Pos.CENTER);
    }

    public HBox getView() {
        return container;
    }
}