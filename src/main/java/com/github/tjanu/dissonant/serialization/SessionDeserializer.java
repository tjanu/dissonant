package com.github.tjanu.dissonant.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.tjanu.dissonant.Session;
import com.github.tjanu.dissonant.audio.Playlist;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SessionDeserializer extends JsonDeserializer<Session> {
    @Override
    public Session deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        var node = jsonParser.getCodec().readTree(jsonParser);
        Session s = new Session();
        s.musicBot.get().token.set(((ValueNode)node.get("music")).asText());
        if (s.musicBot.get().token.get().equals("null")) {
            s.musicBot.get().token.set("");
        }
        s.soundboardBot.get().token.set(((ValueNode)node.get("soundboard")).asText());
        if (s.soundboardBot.get().token.get().equals("null")) {
            s.soundboardBot.get().token.set("");
        }
        ((ArrayNode)node.get("playlists")).forEach(pl -> {
            var name = pl.get("name").asText();
            var tracks = StreamSupport.stream(pl.get("tracks").spliterator(), false).map(n -> new Playlist.Track(n.get("name").asText(), n.get("url").asText())).collect(Collectors.toList());
            s.addPlaylist(name, tracks);
        });
        ((ArrayNode)node.get("sounds")).forEach(sound -> {
            s.soundboard.get().addSound(sound.get("name").asText(), sound.get("url").asText());
        });
        s.dirty.set(false);
        return s;
    }
}
