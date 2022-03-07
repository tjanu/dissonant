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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class Soundboard implements AudioLoadResultHandler {
    public final ObjectProperty<AudioPlayer> player = new SimpleObjectProperty<>();
    public final IntegerProperty playing = new SimpleIntegerProperty(-1);
    private final ListProperty<String> soundNames = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> soundUrls = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BiConsumer<String, AudioLoadResultHandler> loader;

    public Soundboard(BiConsumer<String, AudioLoadResultHandler> loader) {
        this.loader = loader;
        final BooleanProperty inPlayingChanged = new SimpleBooleanProperty(false);
        final AudioEventAdapter eventHandler = new AudioEventAdapter() {
            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                if (!inPlayingChanged.get()) {
                    playing.set(-1);
                }
            }
        };
        Optional.ofNullable(player.get()).ifPresent(player -> {
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
        playing.addListener((obs, o, n) -> {
            inPlayingChanged.set(true);
            try {
                if (o != null) {
                    player.get().stopTrack();
                }
                if (n == null) {
                    return;
                }
                loader.accept(soundUrls.get(n.intValue()), this);
            } finally {
                inPlayingChanged.set(false);
            }
        });
    }

    public void addSound(String id, String url) {
        soundNames.add(id);
        soundUrls.add(url);
    }

    public void changeName(int index, String id) {
        soundNames.set(index, id);
    }

    public void changeUrl(int index, String url) {
        soundUrls.set(index, url);
    }

    public void moveSound(int from, int to) {
        String fromName = soundNames.get(from);
        String fromUrl = soundUrls.get(from);
        soundNames.remove(from);
        soundUrls.remove(from);
        soundNames.add(to, fromName);
        soundUrls.set(to, fromUrl);
    }

    public ReadOnlyListProperty<String> sounds() {
        return soundNames;
    }

    public ReadOnlyListProperty<String> urls() {
        return soundUrls;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        player.get().playTrack(audioTrack);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        player.get().playTrack(audioPlaylist.getSelectedTrack());
    }

    @Override
    public void noMatches() {
        // TODO show no audio to extract
    }

    @Override
    public void loadFailed(FriendlyException e) {
        // TODO show load failed
    }
}
