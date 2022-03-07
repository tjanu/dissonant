package com.github.tjanu.dissonant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.tjanu.dissonant.audio.Soundboard;
import com.github.tjanu.dissonant.audio.Playlist;
import com.github.tjanu.dissonant.audio.LavaPlayerAudioProvider;
import com.github.tjanu.dissonant.serialization.SessionDeserializer;
import com.github.tjanu.dissonant.serialization.SessionSerializer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import java.nio.file.Path;
import java.util.List;

@JsonSerialize(using= SessionSerializer.class)
@JsonDeserialize(using= SessionDeserializer.class)
public class Session {
    public final ReadOnlyObjectProperty<Bot> musicBot = new SimpleObjectProperty<>(new Bot());
    public final ReadOnlyObjectProperty<Bot> soundboardBot = new SimpleObjectProperty<>(new Bot());

    public final ListProperty<Playlist> playlists = new SimpleListProperty<>(FXCollections.observableArrayList());
    public final ReadOnlyObjectProperty<Soundboard> soundboard;

    @JsonIgnore
    public final BooleanProperty dirty = new SimpleBooleanProperty(false);
    @JsonIgnore
    public final ObjectProperty<Path> savePath = new SimpleObjectProperty<>();

    @JsonIgnore
    public final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    @JsonIgnore
    private final AudioPlayer musicPlayer;

    public Session() {
        musicBot.get().token.addListener((observable, oldValue, newValue) -> dirty.setValue(true));
        soundboardBot.get().token.addListener((observable, oldValue, newValue) -> dirty.setValue(true));
        playlists.addListener((observable, oldValue, newValue) -> {
            dirty.setValue(true);
            newValue.addListener((ListChangeListener<Playlist>)(change -> {
                dirty.setValue(true);
                while (change.next()) {
                    if (!change.wasAdded()) {
                        continue;
                    }
                    change.getAddedSubList().forEach(pl -> pl.tracks.addListener((ListChangeListener<Playlist.Track>)(c -> dirty.setValue(true))));
                }
            }));
        });
        soundboard = new SimpleObjectProperty<>(new Soundboard(playerManager::loadItem));
        soundboard.get().sounds().addListener((obs, old, n) -> dirty.set(true));
        soundboard.get().urls().addListener((obs, old, n) -> dirty.set(true));

        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        musicPlayer = playerManager.createPlayer();
        AudioPlayer soundboardPlayer = playerManager.createPlayer();
        musicBot.get().audioProvider.setValue(new LavaPlayerAudioProvider(musicPlayer));
        soundboardBot.get().audioProvider.setValue(new LavaPlayerAudioProvider(soundboardPlayer));

        soundboard.get().player.set(soundboardPlayer);

        // hard coded stuffs for testing
        /*
        addPlaylist("Inge", List.of(new Playlist.Track("Wenn Inge tanzt", "https://www.youtube.com/watch?v=4fXvJHrbUTA")));
        addPlaylist("Electro Swing", List.of(new Playlist.Track("Swing 1", "https://www.youtube.com/watch?v=1hY8f20tqlI"), new Playlist.Track("Swing 2", "https://www.youtube.com/watch?v=V_5qcSZBvYA")));

        soundboard.get().addSound("Telefon", "C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-1-Telefon_Message.mp3");
        soundboard.get().addSound("Keuchen", "C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-2-Telefon-Keuchen.mp3");
        soundboard.get().addSound("Telefon2", "C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-1-Telefon_Message.mp3");
        soundboard.get().addSound("Keuchen2", "C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-2-Telefon-Keuchen.mp3");
        soundboard.get().addSound("Telefon3", "C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-1-Telefon_Message.mp3");
        soundboard.get().addSound("Keuchen3", "file://C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-2-Telefon-Keuchen.mp3");
        soundboard.get().addSound("Telefon4", "file://C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-1-Telefon_Message.mp3");
        soundboard.get().addSound("Keuchen4", "file://C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-2-Telefon-Keuchen.mp3");
        soundboard.get().addSound("Telefon5", "file://C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-1-Telefon_Message.mp3");
        soundboard.get().addSound("Keuchen5", "file://C:\\Users\\thoma\\OneDrive\\Dokumente\\Cthulhu_stuff_from_pegasus\\Abenteuer\\Davenport_Chronik\\Requiem-Sound-2-Telefon-Keuchen.mp3");
         */
    }

    public String serialize() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Session deserialize(String serialization) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(serialization, Session.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void addPlaylist(String name, List<Playlist.Track> tracks) {
        var playlist = new Playlist(name, playerManager::loadItem);
        playlist.player.set(musicPlayer);
        playlist.tracks.addAll(tracks);
        playlists.add(playlist);
    }
}
