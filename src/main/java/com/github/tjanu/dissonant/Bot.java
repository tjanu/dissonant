package com.github.tjanu.dissonant;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.voice.AudioProvider;
import javafx.application.Platform;
import javafx.beans.property.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public class Bot {
    public interface Command {
        Mono<Void> execute(MessageCreateEvent event);
    }

    private final ObjectProperty<GatewayDiscordClient> client = new SimpleObjectProperty<>();
    public final StringProperty token = new SimpleStringProperty();
    public final ObjectProperty<AudioProvider> audioProvider = new SimpleObjectProperty<>();
    public final ListProperty<String> allowedMembers = new SimpleListProperty<>();
    public final BooleanProperty connectable = new SimpleBooleanProperty();
    public final BooleanProperty connected = new SimpleBooleanProperty();

    public Bot() {
        token.addListener((obs, o, n) -> {
            var i = 4;
        });
        connected.addListener((obs, o, n) -> {
            var i = 5;
        });
        connectable.bind(token.isNotNull().and(token.isNotEmpty()).and(audioProvider.isNotNull()));
        connected.bind(client.isNotNull());

        final Map<String, Command> commands = Map.of("join", event -> Mono.justOrEmpty(event.getMember())
                .filter(member -> allowedMembers.isEmpty() || allowedMembers.contains(member.getDisplayName()))
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(channel -> channel.join().withProvider(audioProvider.get()).then()));

        client.addListener((observableValue, oldClient, newClient) -> {
            if (newClient == null) {
                return;
            }
            newClient.getEventDispatcher()
                    .on(MessageCreateEvent.class)
                    .flatMap(event -> Mono.just(event.getMessage().getContent())
                            .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                    .filter(entry -> content.startsWith("!" + entry.getKey()))
                                    .flatMap(entry -> entry.getValue().execute(event)).next())).subscribe();

        });
    }

    public void connect() {
        if (!connectable.get()) {
            throw new IllegalStateException("Not connectable, set token and audio provider first");
        }
        asyncDisconnect().doFinally(unused -> asyncConnect().block()).block();
    }

    public void disconnect() {
        asyncDisconnect().block();
    }

    private Mono<GatewayDiscordClient> asyncConnect() {
        return DiscordClientBuilder.create(token.get()).build().login().doOnSuccess(c -> Platform.runLater(() -> client.setValue(c)));
    }

    private Mono<Void> asyncDisconnect() {
        if (client.isNotNull().get()) {
            return client.get().logout().then(client.get().onDisconnect().doFinally((unused) -> Platform.runLater(() -> client.set(null))));
        }
        return Mono.empty();
    }
}
