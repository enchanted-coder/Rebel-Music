package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;

import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NonNls;
import java.security.SecureRandom;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener {

    private SearchView searchView;

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView endtime, startTime;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImg;
    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;

    private int loopCounter = 99;
    private int shuffleCounter = 99;

    SecureRandom rand = new SecureRandom();

    private void checkReadStoragePermissions() {

        if (Utils.isTiramisu()) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                onPermissionGranted();
            }
        } else if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }


    }

    @TargetApi(23)
    private void showPermissionRationale() {
        AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setIcon(R.drawable.ic_folder);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.perm_rationale));
        builder.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final int READ_FILES_CODE = 2588;
                        if (Utils.isTiramisu()) {
                            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}
                                    , READ_FILES_CODE);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                                    , READ_FILES_CODE);
                        }
                    }
                });
        builder.setCanceledOnTouchOutside(false);
        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    private void onPermissionGranted() {
        getMusicFiles();
    }




    // start here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();



        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);

        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView prevBtn = findViewById(R.id.previousBtn);
        final ImageView loopBtn = findViewById(R.id.loopBtn);
        final ImageView shuffleBtn = findViewById(R.id.shuffleOffBtn);

        playerSeekBar = findViewById(R.id.playerSeekBar);
        startTime = findViewById(R.id.startTime);
        endtime = findViewById(R.id.endTime);
        mediaPlayer = new MediaPlayer();

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (Utils.isTiramisu()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                getMusicFiles();
            } else {
                checkReadStoragePermissions();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getMusicFiles();
            } else {
                checkReadStoragePermissions();
            }
        }



        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (shuffleCounter % 2 == 0) {
                    int upperbounds = musicLists.size();
                    int nextSongListPosition = rand.nextInt(upperbounds);

                    if(nextSongListPosition >= musicLists.size()) {
                        nextSongListPosition = 0;
                    }
                    isPlaying = false;
                    mediaPlayer.pause();

                    musicLists.get(currentSongListPosition).setPlaying(false);
                    musicLists.get(nextSongListPosition).setPlaying(true);

                    musicAdapter.updateList(musicLists);
                    musicRecyclerView.scrollToPosition(nextSongListPosition);

                    onChanged(nextSongListPosition);


                } else if (shuffleCounter % 2 != 0){

                    int nextSongListPosition = currentSongListPosition+1;

                    if(nextSongListPosition >= musicLists.size()) {
                        nextSongListPosition = 0;
                    }
                    isPlaying = false;
                    mediaPlayer.pause();

                    musicLists.get(currentSongListPosition).setPlaying(false);
                    musicLists.get(nextSongListPosition).setPlaying(true);

                    musicAdapter.updateList(musicLists);
                    musicRecyclerView.scrollToPosition(nextSongListPosition);

                    onChanged(nextSongListPosition);

                }


            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevSongListPosition = currentSongListPosition-1;

                if(prevSongListPosition < 0) {
                    prevSongListPosition = musicLists.size()-1;
                }
                isPlaying = false;
                mediaPlayer.pause();

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(prevSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);
                musicRecyclerView.scrollToPosition(prevSongListPosition);

                onChanged(prevSongListPosition);
            }
        });

        // loop counter
        // if the loopCounter variable is divisible by 2 then
        // a loop is implemented
        loopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (loopCounter % 2 != 0) {
                    loopBtn.setImageResource(R.drawable.loop_button_on);
                    loopCounter++;

                } else if (loopCounter % 2 == 0){
                    loopBtn.setImageResource(R.drawable.loop_button);
                    loopCounter++;
                }

                 // increments the loop by 1 value

            }
        });


        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (shuffleCounter % 2 != 0) {
                    shuffleBtn.setImageResource(R.drawable.shuffle_on_icon);
                    shuffleCounter++;

                } else if (shuffleCounter % 2 == 0){
                    shuffleBtn.setImageResource(R.drawable.shuffle_off_icon);
                    shuffleCounter++;
                }

            }
        });



        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isPlaying) {
                    isPlaying = false;
                    mediaPlayer.pause();
                    playPauseImg.setImageResource(R.drawable.play_icon);
                } else {
                    isPlaying = true;
                    mediaPlayer.start();
                    playPauseImg.setImageResource(R.drawable.pause_icon);
                }
            }
        });

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser) {
                    if(isPlaying) {
                        mediaPlayer.seekTo(progress);
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });


    }

    private void filterList(String text) {
        List<MusicList> filteredList = new ArrayList<>();

        for(MusicList musicList : musicLists) {
            if (musicList.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(musicList);
            }
            if (musicList.getArtist().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(musicList);
            }

        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No track found", Toast.LENGTH_SHORT).show();
        } else {

            musicAdapter.setFilteredList(filteredList);

        }
    }

    @SuppressLint("Range")
    private void getMusicFiles() {


        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA+" Like?", new String[]{"%.mp3%"}, null);

        if(cursor == null) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        } else if (!cursor.moveToNext()) {
            Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
        } else {

            while(cursor.moveToNext()) {
                @SuppressLint("Range") final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                @SuppressLint("Range") final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                @SuppressLint("Range") long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);
                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musicLists.add(musicList);
            }

            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);

            musicRecyclerView.setAdapter(musicAdapter);
        }
        assert cursor != null;
        cursor.close();
    }



    @Override
    public void onChanged(int position) {


        currentSongListPosition = position;

        if (mediaPlayer.isPlaying()) {

            mediaPlayer.pause();

        }
        mediaPlayer.pause();
        mediaPlayer.reset();


        mediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());



        new Thread(() -> {
            try {
                mediaPlayer.setDataSource(MainActivity.this, musicLists.get(position).getMusicFile());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
            }
        }).start();

        mediaPlayer.setOnPreparedListener(mp -> {
            final int getTotalDuration = mp.getDuration();

            String generateDuration = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(getTotalDuration), TimeUnit.MILLISECONDS.toSeconds(getTotalDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));

            endtime.setText(generateDuration);
            isPlaying = true;

            playerSeekBar.setMax(getTotalDuration);
            playPauseImg.setImageResource(R.drawable.pause_icon);

        });

        try {
            Thread.sleep(100);
            mediaPlayer.start();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();

                        String generateDuration = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration), TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)));

                        playerSeekBar.setProgress(getCurrentDuration);

                        startTime.setText(generateDuration);
                    }
                });
            }
        }, 1000, 1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                if (shuffleCounter % 2 != 0) {

                    if (loopCounter % 2 != 0) {

                        mediaPlayer.reset();

                        timer.purge();
                        timer.cancel();

                        isPlaying = false;

                        playPauseImg.setImageResource(R.drawable.play_icon);

                        playerSeekBar.setProgress(0);

                        int nextSongListPosition = currentSongListPosition+1;

                        if(nextSongListPosition >= musicLists.size()) {
                            nextSongListPosition = 0;
                        }

                        musicLists.get(currentSongListPosition).setPlaying(false);
                        musicLists.get(nextSongListPosition).setPlaying(true);

                        musicAdapter.updateList(musicLists);

                        musicRecyclerView.scrollToPosition(nextSongListPosition);

                        onChanged(nextSongListPosition);
                    } else if (loopCounter % 2 == 0){

                        mediaPlayer.reset();

                        timer.purge();
                        timer.cancel();

                        isPlaying = false;


                        playPauseImg.setImageResource(R.drawable.play_icon);
                        playerSeekBar.setProgress(0);

                        int nextSongListPosition = currentSongListPosition;

                        musicLists.get(currentSongListPosition).setPlaying(false);
                        musicLists.get(nextSongListPosition).setPlaying(true);

                        musicAdapter.updateList(musicLists);
                        musicRecyclerView.scrollToPosition(nextSongListPosition);

                        onChanged(nextSongListPosition);
                    }

                } else if (shuffleCounter % 2 == 0){

                    if (loopCounter % 2 != 0) {
                        int upperbound = rand.nextInt(musicLists.size());
                        mediaPlayer.reset();

                        timer.purge();
                        timer.cancel();

                        isPlaying = false;

                        playPauseImg.setImageResource(R.drawable.play_icon);

                        playerSeekBar.setProgress(0);

                        int nextSongListPosition = rand.nextInt(upperbound);

                        if(nextSongListPosition >= musicLists.size()) {
                            nextSongListPosition = 0;
                        }

                        musicLists.get(currentSongListPosition).setPlaying(false);
                        musicLists.get(nextSongListPosition).setPlaying(true);

                        musicAdapter.updateList(musicLists);

                        musicRecyclerView.scrollToPosition(nextSongListPosition);

                        onChanged(nextSongListPosition);
                    } else if (loopCounter % 2 == 0){

                        mediaPlayer.reset();

                        timer.purge();
                        timer.cancel();

                        isPlaying = false;

                        playPauseImg.setImageResource(R.drawable.play_icon);
                        playerSeekBar.setProgress(0);

                        int nextSongListPosition = currentSongListPosition;

                        musicLists.get(currentSongListPosition).setPlaying(false);
                        musicLists.get(nextSongListPosition).setPlaying(true);

                        musicAdapter.updateList(musicLists);
                        musicRecyclerView.scrollToPosition(nextSongListPosition);

                        onChanged(nextSongListPosition);
                    }

                }

            }
        });
    }


}