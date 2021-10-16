package com.github.tjanu.dissonant;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        var root = FXMLLoader.<VBox>load(getClass().getResource("/gui/main.fxml"));
        var scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Dissonant");
        stage.show();
    }
}
