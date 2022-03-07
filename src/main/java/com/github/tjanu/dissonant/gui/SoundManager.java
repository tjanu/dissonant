package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import com.github.tjanu.dissonant.Session;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.util.Optional;
import java.util.function.Consumer;

public class SoundManager {
    private final Application.State state;

    @FXML private HBox rootPane;
    @FXML private Button urlChooser;
    @FXML private TextField url;
    @FXML private TextField name;
    @FXML private Button down;
    @FXML private Button up;
    @FXML private Button add;
    @FXML private ListView<String> nameList;

    private ChangeListener<String> currentUrlListener;
    private ChangeListener<String> currentNameListener;
    private ChangeListener<Number> selectedIndexListener;

    public SoundManager(Application.State state) {
        this.state = state;
    }

    public void initialize() {
        urlChooser.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose sound file");
            var file = chooser.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) {
                url.setText(file.getAbsolutePath());
            }
        });
        Consumer<Session> attachTo = session -> {
            var soundboard = session.soundboard.get();
            nameList.itemsProperty().bind(soundboard.sounds());
            selectedIndexListener = (obs, o, n) -> {
                if (o.intValue() >= 0) {
                    url.textProperty().removeListener(currentUrlListener);
                    name.textProperty().removeListener(currentNameListener);
                    url.setText("");
                    name.setText("");
                }
                if (n.intValue() < 0) {
                    return;
                }
                currentUrlListener = (obs1, o1, n1) -> {
                    soundboard.changeUrl(n.intValue(), n1);
                };
                url.setText(soundboard.urls().get(n.intValue()));
                url.textProperty().addListener(currentUrlListener);
                currentNameListener = (obs1, o1, n1) -> {
                    soundboard.changeName(n.intValue(), n1);
                };
                name.setText(soundboard.sounds().get(n.intValue()));
                name.textProperty().addListener(currentNameListener);
            };
            var selectionModel = nameList.getSelectionModel();
            selectionModel.selectedIndexProperty().addListener(selectedIndexListener);

            down.disableProperty().bind(selectionModel.selectedIndexProperty().lessThan(0).or(selectionModel.selectedIndexProperty().greaterThan(soundboard.sounds().size() - 2)));
            down.setOnAction(e -> {
                var i = selectionModel.getSelectedIndex();
                soundboard.moveSound(i, i + 1);
                selectionModel.select(i + 1);
            });

            up.setOnAction(e -> {
                var i = selectionModel.getSelectedIndex();
                soundboard.moveSound(i, i - 1);
                selectionModel.select(i - 1);
            });
            add.setOnAction(e -> {
                soundboard.addSound("Added", "");
                selectionModel.select(soundboard.sounds().size() - 1);
            });
        };
        state.session.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null)
            {
                nameList.itemsProperty().unbind();
                nameList.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                down.disableProperty().unbind();
            }
            if (newValue == null) {
                return;
            }
            attachTo.accept(newValue);
        });
        Optional.ofNullable(state.session.get()).ifPresent(attachTo);

        nameList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        urlChooser.disableProperty().bind(nameList.getSelectionModel().selectedIndexProperty().lessThan(0));
        up.disableProperty().bind(nameList.getSelectionModel().selectedIndexProperty().lessThan(1));
    }
}
