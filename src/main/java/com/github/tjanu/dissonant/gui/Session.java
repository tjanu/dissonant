package com.github.tjanu.dissonant.gui;

import com.github.tjanu.dissonant.Application;
import com.github.tjanu.dissonant.Bot;
import com.github.tjanu.dissonant.audio.Playlist;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class Session {
    private final Application.State state;
    @FXML private BotControls musicBotController;
    @FXML private BotControls soundboardBotController;

    @FXML private ToggleButton play;
    @FXML private ToggleButton loop;
    @FXML private ComboBox<Playlist> musicPlaylist;
    @FXML private CheckBox stopOnChange;
    @FXML private FlowPane soundboard;

    private final Logger log = LoggerFactory.getLogger(Session.class);

    public Session(Application.State state) { this.state = state; }

    public void initialize() {
        log.debug("initializing");
        final Callback<ListView<Playlist>, ListCell<Playlist>> cellFactory = listView -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist playlist, boolean empty) {
                super.updateItem(playlist, empty);
                setText(playlist == null || empty ? "Choose a playlist" : playlist.name.get());
            }
        };
        log.debug("Setting playlist button cell");
        musicPlaylist.setButtonCell(cellFactory.call(null));
        log.debug("Setting playlist cell factory");
        musicPlaylist.setCellFactory(cellFactory);
        Consumer<com.github.tjanu.dissonant.Session> sessionInitializer = (session) -> {
            if (session == null) {
                log.debug("no session given, not initializing");
                soundboard.getChildren().clear();
                return;
            }
            BiConsumer<BotControls, Bot> botConnector = (controls, bot) -> {
                controls.getToken().textProperty().unbind();
                controls.getToken().textProperty().bindBidirectional(bot.token);
                controls.getConnect().disableProperty().unbind();
                controls.getConnect().selectedProperty().unbind();
                controls.getConnect().disableProperty().bind(bot.connectable.not());
                controls.getConnect().selectedProperty().addListener((obs, o, n) -> {
                    if (n) {
                        bot.connect();
                    } else {
                        bot.disconnect();
                    }
                });
                controls.getConnect().graphicProperty().unbind();
                controls.getConnect().graphicProperty().bind(Bindings.createObjectBinding(() -> {
                    var imageNode = new ImageView();
                    imageNode.setFitHeight(14);
                    imageNode.setPreserveRatio(true);
                    imageNode.setImage(new Image(String.valueOf(getClass().getResource("/icons/discord" + (bot.connected.get() ? "" : "-white") + ".png"))));
                    return (Node)imageNode;
                }, bot.connected));
            };
            botConnector.accept(musicBotController, session.musicBot.get());
            botConnector.accept(soundboardBotController, session.soundboardBot.get());

            Consumer<List<String>> addSoundboardButtons = s -> {
                soundboard.getChildren().clear();
                if (s == null) {
                    return;
                }
                class IndexedSound {
                    public final int index;
                    public final String name;
                    public IndexedSound(int i, String s) { index = i; name = s; }
                }
                IntStream.range(0, s.size()).mapToObj(i -> new IndexedSound(i, s.get(i))).forEach(sound -> {
                    ToggleButton soundButton = new ToggleButton(sound.name);
                    session.soundboard.get().playing.addListener((obs, o, n) -> {
                        if (n.intValue() != sound.index)
                        {
                            soundButton.setSelected(false);
                        }
                    });
                    soundButton.selectedProperty().addListener((obs, o, n) -> {
                        if (n) {
                            session.soundboard.get().playing.set(sound.index);
                        }
                    });
                    soundButton.disableProperty().bind(session.soundboardBot.get().connected.not());
                    soundboard.getChildren().add(soundButton);
                });
                soundboard.layout();
            };
            Optional.ofNullable(session.soundboard.get().sounds().get()).ifPresent(addSoundboardButtons);
            session.soundboard.get().sounds().addListener((observable, old, next) -> addSoundboardButtons.accept(next));

            musicPlaylist.itemsProperty().unbind();
            musicPlaylist.itemsProperty().bind(session.playlists);

            play.disableProperty().unbind();
            play.disableProperty().bind(session.musicBot.get().connected.not().or(musicPlaylist.valueProperty().isNull()));
            loop.disableProperty().unbind();
            loop.disableProperty().bind(session.musicBot.get().connected.not().or(musicPlaylist.valueProperty().isNull()));
            log.debug("Adding listener to music playlist combobox");
            musicPlaylist.valueProperty().addListener((obs, o, n) -> {
                log.debug("Selected playlist changed from {} to {}", o != null ? o.name.get() : "null", n != null ? n.name.get() : "null");
                if (o != null && o.playing.get() && n != null) {
                    log.debug("Got old playlist {} that is currently playing", o.name.get());
                    if (stopOnChange.isSelected()) {
                        log.debug("Should stop on change, doing so");
                        o.playing.setValue(false);
                        play.selectedProperty().unbindBidirectional(o.playing);
                        loop.selectedProperty().unbindBidirectional(o.looping);
                        play.selectedProperty().bindBidirectional(n.playing);
                        loop.selectedProperty().bindBidirectional(n.looping);
                        n.looping.set(o.looping.get());
                        n.playing.setValue(true);
                    } else {
                        log.debug("should not stop on change, registering {} to run after {}", n.name.get(), o.name.get());
                        n.playAfter(o, () -> Platform.runLater(() -> {
                            log.debug("{} started playing after {}", n.name.get(), o.name.get());
                            log.debug("unbinding play selected property");
                            play.selectedProperty().unbindBidirectional(o.playing);
                            log.debug("binding play selected property to {}", n.name.get());
                            play.selectedProperty().bindBidirectional(n.playing);
                            log.debug("unbinding loop selected property");
                            loop.selectedProperty().unbindBidirectional(o.looping);
                            log.debug("binding loop selected property to {}", n.name.get());
                            loop.selectedProperty().bindBidirectional(n.looping);
                            log.debug("setting {}'s looping to {}'s looping: {}", n.name.get(), o.name.get(), o.looping.get());
                            n.looping.set(o.looping.get());
                            log.debug("Starting to play {}", n.name.get());
                            n.playing.set(true);
                        }));
                    }
                    return;
                }
                if (n != null) {
                    log.debug("did not have an old playlist, binding play and loop to {}", n.name.get());
                    play.selectedProperty().bindBidirectional(n.playing);
                    loop.selectedProperty().bindBidirectional(n.looping);
                }
            });
        };
        log.debug("Initializing with current session if present");
        Optional.ofNullable(state.session.get()).ifPresent(sessionInitializer);
        log.debug("Adding session listener");
        state.session.addListener((observable, oldValue, newValue) -> sessionInitializer.accept(newValue));
    }
}
