package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import com.github.tjanu.dissonant.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class Menu {
    private final Application.State state;
    @FXML private MenuItem saveEntry;
    @FXML private MenuItem saveAsEntry;
    @FXML private javafx.scene.control.Menu manageMenu;
    @FXML private MenuItem playlistsEntry;
    @FXML private MenuItem soundsEntry;

    public Menu(Application.State state) {
        this.state = state;
    }

    public void initialize() {
        Stream.of(saveEntry, saveAsEntry).forEach(node -> node.setDisable(true));
        Stream.of(manageMenu, playlistsEntry, soundsEntry).forEach(node -> node.disableProperty().bind(state.session.isNull()));
        state.session.addListener((obs, o, n) -> {
            if (n != null) {
                Stream.of(saveEntry, saveAsEntry).forEach(node -> node.disableProperty().bind(n.dirty.not()));
            } else {
                Stream.of(saveEntry, saveAsEntry).forEach(node -> {
                    node.disableProperty().unbind();
                    node.setDisable(true);
                });
            }
        });
    }

    public void create() {
        if (!confirmChangeIfCurrentSessionDirty()) {
            return;
        }
        state.session.setValue(new com.github.tjanu.dissonant.Session());
    }

    public void open() {
        if (!confirmChangeIfCurrentSessionDirty()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open file");
        var file = chooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        var path = file.toPath();
        try {
            var session = Session.deserialize(Files.readString(path, StandardCharsets.UTF_8));
            session.savePath.set(path);
            state.session.set(session);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        var session = state.session.get();
        if (session == null) {
            return;
        }
        if (session.savePath.get() == null) {
            saveAs();
            return;
        }
        var content = session.serialize();
        try {
            Files.writeString(session.savePath.get(), content, StandardCharsets.UTF_8);
            session.dirty.set(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveAs() {
        var session = state.session.get();
        if (session == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save as...");
        var file = chooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        var path = file.toPath();
        session.savePath.set(path);
        var content = session.serialize();
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            session.dirty.set(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (!confirmChangeIfCurrentSessionDirty()) {
            return;
        }
        Platform.exit();
    }

    public void managePlaylists() throws Exception {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Playlists");
        dialog.getDialogPane().setContent(com.github.tjanu.dissonant.Application.<VBox>load(getClass().getResource("/gui/playlistManager.fxml")));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    public void manageSounds() throws Exception {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Sounds");
        dialog.getDialogPane().setContent(com.github.tjanu.dissonant.Application.<HBox>load(getClass().getResource("/gui/soundManager.fxml")));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.showAndWait();
    }

    private boolean confirmChangeIfCurrentSessionDirty() {
        if (state.session.isNotNull().get() && state.session.get().dirty.get())
        {
            return new Alert(Alert.AlertType.CONFIRMATION, "Current session was modified, are you sure?").showAndWait().map(response -> response == ButtonType.OK).orElse(false);
        }
        return true;
    }
}
