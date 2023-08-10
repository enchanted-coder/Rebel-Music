package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private List<MusicList> list;
    private List<MusicList> songList = new ArrayList<>();
    private List<MusicList> filteredList = new ArrayList<>();
    private final Context context;
    private int playingPosition = 0;
    private final SongChangeListener songChangeListener;

    public void setSongList(List<MusicList> songList) {
        this.songList = songList;
        notifyDataSetChanged();
    }

    public void setFilteredList(List<MusicList> filteredList) {
        this.list = filteredList;

        notifyDataSetChanged();

    }


    public MusicAdapter(List<MusicList> list, Context context) {
        this.list = list;
        this.context = context;
        this.songChangeListener = ((SongChangeListener) context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.music_adapter_layout, null), songChangeListener);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MusicList list2 = list.get(position);

        if(list2.isPlaying()) {
            playingPosition = position;
            //holder.rootLayout.setBackgroundResource(R.drawable.round_back_blue_10);
        } else {
            holder.rootLayout.setBackgroundResource(R.drawable.round_back_10);
        }

        String generationDuration = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(Long.parseLong(list2.getDuration())), TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(list2.getDuration())) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(Long.parseLong(list2.getDuration()))));
        holder.title.setText(list2.getTitle());
        holder.artist.setText(list2.getArtist());
        holder.musicDuration.setText(generationDuration);
        holder.albumArt.setImageURI(list2.getAlbumArt());

        holder.itemView.setOnClickListener(v -> {
            //((MainActivity)
            //v.getContext()).updateBottomCardView(list2);
        });


        holder.rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.get(playingPosition).setPlaying(false);
                list2.setPlaying(true);

                songChangeListener.onChanged(position);

                notifyDataSetChanged();
            }
        });
    }



    public void updateList(List<MusicList> List) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }



    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final RelativeLayout rootLayout;
        private final TextView title;
        private final TextView artist;
        private final ImageView albumArt;
        private final TextView musicDuration;

        public MyViewHolder(@NonNull View itemView, SongChangeListener songChangeListener) {
            super(itemView);

            rootLayout = itemView.findViewById(R.id.rootLayout);
            title = itemView.findViewById(R.id.musicTitle);
            artist = itemView.findViewById(R.id.musicArtist);
            musicDuration = itemView.findViewById(R.id.musicDuration);
            albumArt = itemView.findViewById(R.id.albumArt);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    songChangeListener.onChanged(getAdapterPosition());


                }
            });
        }
    }
}
