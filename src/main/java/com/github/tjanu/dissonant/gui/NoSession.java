package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import com.github.tjanu.dissonant.Session;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class NoSession {
    private Application.State state;

    public NoSession(Application.State state) {
        this.state = state;
    }

    public void create(ActionEvent actionEvent) {
        state.session.setValue(new com.github.tjanu.dissonant.Session());
    }

    public void load(ActionEvent actionEvent) {
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
}
