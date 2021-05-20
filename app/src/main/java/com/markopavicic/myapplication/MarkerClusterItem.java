package com.markopavicic.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import org.jetbrains.annotations.NotNull;

public class MarkerClusterItem implements ClusterItem {
    private final LatLng latLng;

    public MarkerClusterItem(LatLng location) {
        this.latLng = location;
    }

    @NonNull
    @NotNull
    @Override

    public LatLng getPosition() {
        return latLng;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public String getSnippet() {
        return "";
    }
}
