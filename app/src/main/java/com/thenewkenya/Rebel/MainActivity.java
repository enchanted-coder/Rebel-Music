package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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

import androidx.annotation.ContentView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;

import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
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
    // searchview
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

    // if loop counter is odd number then the loop is off
    // if loop counter is even number then the loop is on
    private int loopCounter = 99;

    // if shufflecounter is odd number then the shuffle is off
    // but if shufflecounter is even number is even then shuffle is on
    private int shuffleCounter = 99;

    // Secure random class here is used to generate random number that
    // will be used with the shuffle method. I use SecureRandom because it is
    // has less chance of repeating previous number.
    SecureRandom rand = new SecureRandom();

    SwipeRefreshLayout swipeRefreshLayout;



    // start here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
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

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        // Android 13+ requires specific storage permissions
        // Android 12 and earlier use READ_EXTERNAL_STORAGE for all.
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

        ContextCompat.startForegroundService(
                MainActivity.this.getApplicationContext(),
                new Intent(MainActivity.this.getApplicationContext(), MediaSessionService.class));


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMusicFiles();
                swipeRefreshLayout.setRefreshing(false);

            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // runs shuffle counter to check if shuffle button is on
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

        // For the search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // we return false here unless we wanted a result only after submitting.
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            // To display search results everytime a new letter is input
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
            // to search by title and also to search in lowercase letters
            if (musicList.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(musicList);
            }
            // To search by artist and also to search in lowercase letters
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
        String[] projection = new String[]{
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,  // error from android side, it works < 29
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,

                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
        String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        //Cursor cursor = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA+" Like?", new String[]{"%.mp3%"}, sortOrder);
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder);
        int albumIdInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
        int albumInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);

        if(cursor == null) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        } else if (!cursor.moveToNext()) {
            Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
        } else {

            while(cursor.moveToNext()) {
                @SuppressLint("Range") final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                @SuppressLint("Range") final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                @SuppressLint("Range") long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String album = cursor.getString(albumInd);
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);

                long albumId = cursor.getLong(albumIdInd);
                Uri albumArt = Uri.parse("");
                albumArt = ContentUris.withAppendedId(Uri.parse(getResources().getString(R.string.album_art_dir)), albumId);
                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }


                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri, albumArt);
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
            Thread.sleep(100); // 100ms delay to prevent event state overriding each other and cause a crash
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


   /* public void launchPlayer(View v) {
        Intent i = new Intent(this, PlayerView);
        startActivity(i);
    }
*/



    // PERMISSIONS ARE ALL BELOW

    private void checkReadStoragePermissions() {

        if (Utils.isTiramisu()) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                getMusicFiles();
            }
        } else if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                getMusicFiles();
            }
        } else {
            getMusicFiles();
        }


    }


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

                        recreate();

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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    // needs modification
    private void onPermissionGranted() {
        getMusicFiles();

    }


}