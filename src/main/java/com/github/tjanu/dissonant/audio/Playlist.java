package com.github.tjanu.dissonant.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class Playlist implements AudioLoadResultHandler {
    public static final class Track {
        public Track() {}
        public Track(String name, String url) {
            this.name.setValue(name);
            this.url.setValue(url);
        }
        public final StringProperty name = new SimpleStringProperty();
        public final StringProperty url = new SimpleStringProperty();
    }
    private interface PlayAfterListener {
        void onDoneWithCurrentTrack();
    }
    public final ReadOnlyStringProperty name;
    public final ObjectProperty<AudioPlayer> player = new SimpleObjectProperty<>();
    public final ObservableList<Track> tracks = new SimpleListProperty<>(FXCollections.observableArrayList());
    public final StringProperty currentlyPlaying = new SimpleStringProperty("");
    public final IntegerProperty currentIndex = new SimpleIntegerProperty(-1);
    public final BooleanProperty playing = new SimpleBooleanProperty();
    public final BooleanProperty looping = new SimpleBooleanProperty();

    private static final Logger log = LoggerFactory.getLogger(Playlist.class);
    private final BiConsumer<String, AudioLoadResultHandler> loader;
    private PlayAfterListener afterListener = null;
    private AtomicBoolean inNextTrack = new AtomicBoolean(false);

    public Playlist(String name, BiConsumer<String, AudioLoadResultHandler> loader) {
        this.name = new SimpleStringProperty(name);
        this.loader = loader;
        playing.addListener((obs, o, n) -> {
            log.debug("[{}] Playing state changed from {} to {}", this.name.get(), o, n);
            if (n) {
                log.debug("[{}] trying to play next track", this.name.get());
                nextTrack();
            } else {
                log.debug("[{}] stopping player and setting current index to -1", this.name.get());
                player.get().stopTrack();
                currentIndex.set(-1);
            }
        });
        final AudioEventAdapter eventHandler = new AudioEventAdapter() {
            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                if (inNextTrack.get()) {
                    return;
                }
                log.debug("Track ended");
                if (!endReason.mayStartNext) {
                    log.debug("end reason indicates next track should not be played");
                    playing.setValue(false);
                    return;
                }
                log.debug("Trying to play next track");
                nextTrack();
            }

            @Override
            public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
                log.debug("Track stuck, handling it as track end");
                onTrackEnd(player, track, AudioTrackEndReason.STOPPED);
            }
        };
        Optional.ofNullable(player.get()).ifPresent(player -> {
            log.debug("Adding event listener to existing player");
            player.addListener(eventHandler);
        });
        player.addListener((obs, o, n) -> {
            if (o != null) {
                o.removeListener(eventHandler);
            }
            if (n != null) {
                n.addListener(eventHandler);
            }
        });
    }

    public void playAfter(Playlist playlist, Runnable afterRunning) {
        log.debug("[{}] requested to play after {}", name.get(), playlist.name.get());
        playlist.afterListener = () -> {
            log.debug("[{}] {} finished its current track, playing my next one", name.get(), playlist.name.get());
            playlist.afterListener = null;
            if (afterRunning != null) {
                log.debug("[{}] afterRunning was not null for this play after request, executing", name.get());
                afterRunning.run();
            }
        };
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        player.get().playTrack(audioTrack);
        if (currentIndex.get() >= 0 && currentIndex.get() < tracks.size()) {
            currentlyPlaying.setValue(tracks.get(currentIndex.get()).name.get());
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        audioPlaylist.getTracks().forEach(this::trackLoaded);
    }

    @Override
    public void noMatches() {
        nextTrack();
    }

    @Override
    public void loadFailed(FriendlyException e) {
        nextTrack();
    }

    private void nextTrack() {
        inNextTrack.set(true);
        try {
            log.debug("[{}] nextTrack() start", name.get());
            if (afterListener != null) {
                player.get().stopTrack();
                log.debug("[{}] got an after listener, calling", name.get());
                afterListener.onDoneWithCurrentTrack();
                log.debug("[{}] nextTrack() done", name.get());
                return;
            }
            if (!playing.get() || tracks.isEmpty()) {
                log.debug("[{}] nextTrack() done (not playing or no tracks", name.get());
                return;
            }
            log.debug("[{}] advancing index from {}", name.get(), currentIndex.get());
            currentIndex.set(currentIndex.get() + 1);
            log.debug("[{}] checking for looping: {}", name.get(), looping.get());
            if (looping.get()) {
                log.debug("[{}] looping is true, applying % {}", name.get(), tracks.size());
                currentIndex.set(currentIndex.get() % tracks.size());
            }
            log.debug("[{}] checking index is in range for tracks", name.get());
            if (currentIndex.get() >= 0 && currentIndex.get() < tracks.size()) {
                log.debug("[{}] index in range, loading track <{}>", name.get(), tracks.get(currentIndex.get()).url.get());
                loader.accept(tracks.get(currentIndex.get()).url.get(), this);
            } else {
                log.debug("[{}] index not in range, stopping to play", name.get());
                currentIndex.set(-1);
                playing.set(false);
            }
            log.debug("[{}] nextTrack() done", name.get());
        } finally {
            inNextTrack.set(false);
        }
    }
}
