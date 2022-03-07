# Dissonant

a locally run music and soundboard bot for discord

## State
- heavily experimental
- jpackage generated installer does not work yet, I don't know why
- basically no documentation, error handling or logging

## Usage
- should be able to run using `mvn javafx:run`
- create 2 discord applications with accompanying bots using one of the many guides out there. For Permisions, be sure to add Connecting and Speaking for voice channels. Everything else is not needed.
- add the tokens from both clients in the UI, be sure to save the configuration if you don't want to keep copy/pasting
- create playlists for the music bot, add sounds for the soundbar
- after saving, you can easily edit the json file by hand. The structure is very simple.
- I'd recommend running the software from an IDE like intellij for now, that way you at least get logs if something goes wrong (especially when playing youtube URLs)
- Prefer local audio files over youtube links - downloading, transcoding and uploading may easily lead to "hiccups" in playing.

## Supported audio / URLs
- the bot uses [lavaplayer](https://github.com/sedmelluq/lavaplayer), check supported stuff there