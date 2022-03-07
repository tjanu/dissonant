package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class MainWindow {
    private Application.State state;
    @FXML private Menu menuController;
    @FXML private NoSession noSessionController;
    @FXML private Session sessionController;
    @FXML private VBox session;
    @FXML private VBox noSession;

    public MainWindow(Application.State state) {
        this.state = state;
    }

    public void initialize() {
        session.visibleProperty().bind(state.session.isNotNull());
        session.managedProperty().bind(session.visibleProperty());
        noSession.visibleProperty().bind(state.session.isNull());
        noSession.managedProperty().bind(noSession.visibleProperty());
    }
}
