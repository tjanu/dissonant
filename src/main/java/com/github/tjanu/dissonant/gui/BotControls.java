package com.github.tjanu.dissonant.gui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class BotControls {
    @FXML private TextField token;
    @FXML private ToggleButton connect;

    public TextField getToken() {
        return token;
    }

    public ToggleButton getConnect() {
        return connect;
    }
}
