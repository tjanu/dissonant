package com.github.tjanu.dissonant.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.tjanu.dissonant.Session;

import java.io.IOException;
import java.util.stream.IntStream;

public class SessionSerializer extends JsonSerializer<Session> {
    @Override
    public void serialize(Session session, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (session == null) {
            jsonGenerator.writeNull();
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("music", session.musicBot.get().token.get());
        jsonGenerator.writeStringField("soundboard", session.soundboardBot.get().token.get());
        jsonGenerator.writeArrayFieldStart("playlists");
        session.playlists.forEach(playlist -> {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("name", playlist.name.get());
                jsonGenerator.writeArrayFieldStart("tracks");
                playlist.tracks.forEach(track -> {
                    try {
                        jsonGenerator.writeStartObject();
                        jsonGenerator.writeStringField("name", track.name.get());
                        jsonGenerator.writeStringField("url", track.url.get());
                        jsonGenerator.writeEndObject();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                jsonGenerator.writeEndArray();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        jsonGenerator.writeEndArray();
        jsonGenerator.writeArrayFieldStart("sounds");
        IntStream.range(0, session.soundboard.get().sounds().size()).forEach(index -> {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("name", session.soundboard.get().sounds().get(index));
                jsonGenerator.writeStringField("url", session.soundboard.get().urls().get(index));
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
