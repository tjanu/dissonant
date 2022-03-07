package com.github.tjanu.dissonant;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;

public class Application extends javafx.application.Application {
    public static class State {
        public ObjectProperty<Session> session = new SimpleObjectProperty<>();
    }

    private static final State state = new State();
    private static Callback<Class<?>, Object> controllerFactory = cls -> {
        try {
            for (var constructor : cls.getConstructors()) {
                switch (constructor.getParameterCount()) {
                    case 0:
                        return constructor.newInstance();
                    case 1:
                        if (constructor.getParameterTypes()[0].equals(State.class)) {
                            return constructor.newInstance(state);
                        }
                    default:
                }
            }
            throw new InstantiationException("Failed to instantiate controller of type " + cls.getCanonicalName());
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate controller", t);
        }
    };

    public static <T> T load(URL url) throws Exception {
        var loader = new FXMLLoader(url);
        loader.setControllerFactory(controllerFactory);
        return loader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        var root = Application.<VBox>load(getClass().getResource("/gui/main.fxml"));
        var scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Dissonant");
        state.session.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            observable.getValue().dirty.addListener((obs, o, n) -> {
                stage.titleProperty().setValue(n ? "Dissonant*" : "Dissonant");
            });
        });
        stage.show();
    }
}
