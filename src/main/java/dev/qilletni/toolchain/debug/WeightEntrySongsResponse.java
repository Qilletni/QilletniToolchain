package dev.qilletni.toolchain.debug;

import dev.qilletni.impl.debug.WeightEntrySongs;

import java.util.List;

/**
 * The body of a {@code qilletni/weightEntrySongs} response.
 */
public class WeightEntrySongsResponse {
    public List<Song> songs;
    /** If every song is equally likely when the entry is picked. */
    public boolean evenChance;

    public static class Song {
        public String song;
        public String trackId;
    }

    public static WeightEntrySongsResponse of(WeightEntrySongs entrySongs) {
        var response = new WeightEntrySongsResponse();
        response.songs = entrySongs.songs().stream().map(from -> {
            var song = new Song();
            song.song = from.song();
            song.trackId = from.trackId();
            return song;
        }).toList();
        response.evenChance = entrySongs.evenChance();
        return response;
    }
}
