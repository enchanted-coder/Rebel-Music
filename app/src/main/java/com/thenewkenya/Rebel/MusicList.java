package com.thenewkenya.Rebel;

import android.net.Uri;

public class MusicList {

    private String title, artist, duration;
    private int AlbumArtResId;
    private boolean isPlaying;

    private Uri musicFile;
    private Uri albumArt;


    public MusicList(String title, int albumArtResId, String artist, String duration, boolean isPlaying, Uri musicFile, Uri albumArt) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.isPlaying = isPlaying;
        this.musicFile = musicFile;
        this.albumArt = albumArt;
        this.AlbumArtResId = albumArtResId;


    }
    public int getAlbumArtResId() {
        return AlbumArtResId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDuration() {
        return duration;
    }


    public boolean isPlaying() {
        return isPlaying;
    }

    public Uri getMusicFile() {
        return musicFile;
    }
    public Uri getAlbumArt() { return albumArt; }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
