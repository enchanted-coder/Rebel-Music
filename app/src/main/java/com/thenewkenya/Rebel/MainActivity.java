package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.Manifest;
import androidx.palette.graphics.Palette;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;

import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    private boolean isPlaying = false;

    private ProgressBar progressBar;

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

    private CardView bottomCardView;
    private ImageView albumArtImageView;





    // start here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();

        bottomCardView = findViewById(R.id.bottomCardView);



        mediaPlayer = new MediaPlayer();


        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));



        // Android 13+ requires specific storage permissions
        // Android 12 and earlier use READ_EXTERNAL_STORAGE for all.
        if (Utils.isTiramisu()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                checkReadStoragePermissions();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                checkReadStoragePermissions();
            }
        }

        // to be worked on later
        /*
        if (Utils.isTiramisu()) {
            ContextCompat.startForegroundService(
                    MainActivity.this.getApplicationContext(),
                    new Intent(MainActivity.this.getApplicationContext(), MediaSessionService.class));
        }*/







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
        bottomCardView.setVisibility(View.INVISIBLE);
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
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                MediaStore.Audio.Media.DATA + " LIKE '%.mp3' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.flac')";
        String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

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


                int getAlbumResId = 0;
                final MusicList musicList = new MusicList(getMusicFileName, getAlbumResId, getArtistName, getDuration, false, musicFileUri, albumArt);
                musicLists.add(musicList);

            }

            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);

            musicRecyclerView.setAdapter(musicAdapter);
        }
        assert cursor != null;
        cursor.close();
    }

    void updateBottomCardView(MusicList musicList) {
        bottomCardView.setVisibility(View.VISIBLE);

        ImageView album_art = bottomCardView.findViewById(R.id.album_art);
        TextView textViewTitle = bottomCardView.findViewById(R.id.textViewTitle);
        TextView textViewArtist = bottomCardView.findViewById(R.id.textViewArtist);
        ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
        //LinearProgressIndicator media_player_bar_progress_indicator = bottomCardView.findViewById(R.id.media_player_bar_progress_indicator);

        textViewTitle.setText(musicList.getTitle());
        textViewArtist.setText(musicList.getArtist());
        album_art.setImageURI(musicList.getAlbumArt());


        btn_play_pause.setOnClickListener(view -> {
            if (isPlaying) {
                pausePlayback();
            } else {
                startPlayback();
            }
        });


    }

    Handler handler = new Handler();

    Runnable updateLinearProgressIndicator = new Runnable() {
        @Override
        public void run() {
            ProgressBar progressBar = findViewById(R.id.media_player_bar_progress_indicator);
            //LinearProgressIndicator media_player_bar_progress_indicator = findViewById(R.id.media_player_bar_progress_indicator);
            int currentPosition = mediaPlayer.getCurrentPosition();
            progressBar.setProgress(currentPosition);

            handler.postDelayed(this, 1000);
        }
    };
    private void startUpdatingProgressBar() {
        handler.postDelayed(updateLinearProgressIndicator, 0);
    }

    private void stopUpdatingProgressBar() {
        handler.removeCallbacks(updateLinearProgressIndicator);
    }

    private void startPlayback() {
        startUpdatingProgressBar();
        ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
        mediaPlayer.start();
        isPlaying = true;
        btn_play_pause.setImageResource(R.drawable.pause_icon);
    }

    private void pausePlayback() {
        stopUpdatingProgressBar();
        ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
        mediaPlayer.pause();
        isPlaying = false;
        btn_play_pause.setImageResource(R.drawable.play_icon);
    }





    @Override
    public void onChanged(int position) {
        ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
        startUpdatingProgressBar();
        btn_play_pause.setImageResource(R.drawable.pause_icon);


        if (position >=0 && position < musicLists.size()) {
            MusicList musicList = musicLists.get(position);
            updateBottomCardView(musicList);
        }

        progressBar = findViewById(R.id.media_player_bar_progress_indicator);

        currentSongListPosition = position;


        mediaPlayer.reset();

        mediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());



        new Thread(() -> {
            while(mediaPlayer.isPlaying()) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                runOnUiThread(() -> {
                    progressBar.setProgress(currentPosition);
                });
            }
            try {
                mediaPlayer.setDataSource(MainActivity.this, musicLists.get(position).getMusicFile());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
            }
        }).start();

        mediaPlayer.setOnPreparedListener(mp -> {
            progressBar.setMax(mediaPlayer.getDuration());
            final int getTotalDuration = mp.getDuration();

            String generateDuration = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(getTotalDuration), TimeUnit.MILLISECONDS.toSeconds(getTotalDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));


            isPlaying = true;



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


                    }
                });
            }
        }, 1000, 1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.reset();

                timer.purge();
                timer.cancel();

                isPlaying = false;



                int nextSongListPosition = currentSongListPosition+1;

                if(nextSongListPosition >= musicLists.size()) {
                    nextSongListPosition = 0;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(nextSongListPosition);

                onChanged(nextSongListPosition);



            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
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
                permissionRationale();
            } else {
                onPermissionGranted();
            }
        } else if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionRationale();
            } else {
                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }


    }

    private void permissionRationale() {
        Intent intent = new Intent(this, PermissionRationale.class);
        startActivity(intent);
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
            permissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    // needs modification
    private void onPermissionGranted() {

        getMusicFiles();

    }




}