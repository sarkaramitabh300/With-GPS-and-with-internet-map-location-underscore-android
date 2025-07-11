package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.DownloadedMap;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying downloaded maps list
 */
public class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.MapViewHolder> {
    
    private List<DownloadedMap> maps;
    private OnMapClickListener listener;

    public interface OnMapClickListener {
        void onMapClick(DownloadedMap map);
        void onMapLongClick(DownloadedMap map);
    }

    public MapListAdapter(OnMapClickListener listener) {
        this.maps = new ArrayList<>();
        this.listener = listener;
    }

    public void setMaps(List<DownloadedMap> maps) {
        this.maps = maps != null ? maps : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_downloaded_map, parent, false);
        return new MapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MapViewHolder holder, int position) {
        DownloadedMap map = maps.get(position);
        holder.bind(map, listener);
    }

    @Override
    public int getItemCount() {
        return maps.size();
    }

    static class MapViewHolder extends RecyclerView.ViewHolder {
        private TextView labelTextView;
        private TextView descriptionTextView;
        private TextView locationTextView;
        private TextView dateTextView;
        private TextView mapTypeTextView;
        private TextView statusTextView;

        public MapViewHolder(@NonNull View itemView) {
            super(itemView);
            labelTextView = itemView.findViewById(R.id.tv_map_label);
            descriptionTextView = itemView.findViewById(R.id.tv_map_description);
            locationTextView = itemView.findViewById(R.id.tv_map_location);
            dateTextView = itemView.findViewById(R.id.tv_map_date);
            mapTypeTextView = itemView.findViewById(R.id.tv_map_type);
            statusTextView = itemView.findViewById(R.id.tv_map_status);
        }

        public void bind(DownloadedMap map, OnMapClickListener listener) {
            // Set map label
            labelTextView.setText(map.getLabel());
            
            // Set description (hide if empty)
            if (map.getDescription() != null && !map.getDescription().trim().isEmpty()) {
                descriptionTextView.setText(map.getDescription());
                descriptionTextView.setVisibility(View.VISIBLE);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }
            
            // Set location info
            locationTextView.setText(map.getBoundsDescription());
            
            // Set download date
            String formattedDate = android.text.format.DateFormat.format("MMM dd, yyyy", map.getDownloadDate()).toString();
            dateTextView.setText("Downloaded: " + formattedDate);
            
            // Set map type with appropriate styling
            mapTypeTextView.setText(map.getMapType());
            setMapTypeStyle(mapTypeTextView, map.getMapType());
            
            // Set status
            if (map.isAvailableOffline()) {
                statusTextView.setText("✓ Available Offline");
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                statusTextView.setText("⚠ Not Available Offline");
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
            }
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMapClick(map);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMapLongClick(map);
                }
                return true;
            });
        }
        
        private void setMapTypeStyle(TextView textView, String mapType) {
            int backgroundColor;
            int textColor = itemView.getContext().getResources().getColor(android.R.color.white);
            
            switch (mapType.toUpperCase()) {
                case "NORMAL":
                    backgroundColor = itemView.getContext().getResources().getColor(android.R.color.holo_blue_light);
                    break;
                case "SATELLITE":
                    backgroundColor = itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
                    break;
                case "HYBRID":
                    backgroundColor = itemView.getContext().getResources().getColor(android.R.color.holo_orange_light);
                    break;
                case "TERRAIN":
                    backgroundColor = itemView.getContext().getResources().getColor(android.R.color.holo_purple);
                    break;
                default:
                    backgroundColor = itemView.getContext().getResources().getColor(android.R.color.darker_gray);
                    break;
            }
            
            textView.setBackgroundColor(backgroundColor);
            textView.setTextColor(textColor);
            textView.setPadding(16, 8, 16, 8);
        }
    }
}
