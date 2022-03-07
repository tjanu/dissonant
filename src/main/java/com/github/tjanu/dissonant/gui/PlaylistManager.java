package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import com.github.tjanu.dissonant.Session;
import com.github.tjanu.dissonant.audio.Playlist;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.Optional;
import java.util.function.Consumer;

public class PlaylistManager {
    private final Application.State state;

    @FXML private VBox rootPane;
    @FXML private ComboBox<Playlist> playlistChooser;
    @FXML private Button urlChooser;
    @FXML private TextField url;
    @FXML private TextField name;
    @FXML private Button down;
    @FXML private Button up;
    @FXML private Button add;
    @FXML private ListView<Playlist.Track> nameList;

    private ChangeListener<String> currentUrlListener;
    private ChangeListener<String> currentNameListener;
    private ChangeListener<Number> selectedIndexListener;

    public PlaylistManager(Application.State state) {
        this.state = state;
    }

    public void initialize() {
        final Callback<ListView<Playlist>, ListCell<Playlist>> cellFactory = listView -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist playlist, boolean empty) {
                super.updateItem(playlist, empty);
                setText(playlist == null || empty ? "Choose a playlist" : playlist.name.get());
            }
        };
        playlistChooser.setButtonCell(cellFactory.call(null));
        playlistChooser.setCellFactory(cellFactory);

        nameList.setCellFactory(listView -> new TextFieldListCell<>(new StringConverter<>() {
            @Override public String toString(Playlist.Track track) {
                return track.name.get();
            }

            @Override public Playlist.Track fromString(String s) {
                return null;
            }
        }));

        urlChooser.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose sound file");
            var file = chooser.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) {
                url.setText(file.getAbsolutePath());
            }
        });

        Consumer<Session> attachTo = session -> {
          playlistChooser.itemsProperty().bind(session.playlists);
          playlistChooser.getSelectionModel().selectedItemProperty().addListener((obsPlaylist, oldPlaylist, newPlaylist) -> {
                  nameList.setItems(newPlaylist.tracks);
                  var selectionModel = nameList.getSelectionModel();
                  if (oldPlaylist != null) {
                      selectionModel.selectedIndexProperty().removeListener(selectedIndexListener);
                  }
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
                      currentUrlListener = (obs1, o1, n1) -> newPlaylist.tracks.get(n.intValue()).url.set(n1);
                      url.setText(newPlaylist.tracks.get(n.intValue()).url.get());
                      url.textProperty().addListener(currentUrlListener);
                      currentNameListener = (obs1, o1, n1) -> newPlaylist.tracks.get(n.intValue()).name.set(n1);
                      name.setText(newPlaylist.tracks.get(n.intValue()).name.get());
                      name.textProperty().addListener(currentNameListener);
                  };
                  selectionModel.selectedIndexProperty().addListener(selectedIndexListener);

                  down.disableProperty().bind(selectionModel.selectedIndexProperty().lessThan(0).or(selectionModel.selectedIndexProperty().greaterThan(newPlaylist.tracks.size() - 2)));
                  down.setOnAction(e -> {
                      var i = selectionModel.getSelectedIndex();
                      var track = newPlaylist.tracks.remove(i);
                      newPlaylist.tracks.add(i + 1, track);
                      selectionModel.select(i + 1);
                  });

                  up.setOnAction(e -> {
                      var i = selectionModel.getSelectedIndex();
                      var track = newPlaylist.tracks.remove(i);
                      newPlaylist.tracks.add(i - 1, track);
                      selectionModel.select(i - 1);
                  });
                  add.setOnAction(e -> {
                      newPlaylist.tracks.add(new Playlist.Track("Added", ""));
                      selectionModel.select(newPlaylist.tracks.size() - 1);
                  });
                  urlChooser.disableProperty().bind(nameList.getSelectionModel().selectedIndexProperty().lessThan(0));
                  up.disableProperty().bind(nameList.getSelectionModel().selectedIndexProperty().lessThan(1));
              });
              nameList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        };

        Optional.ofNullable(state.session.get()).ifPresent(attachTo);
        state.session.addListener((obs, o, n) -> {
            if (o != null) {
                url.textProperty().removeListener(currentUrlListener);
                name.textProperty().removeListener(currentNameListener);
                nameList.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                down.disableProperty().unbind();
            }
            if (n == null) {
                return;
            }
            attachTo.accept(n);
        });
    }
}
