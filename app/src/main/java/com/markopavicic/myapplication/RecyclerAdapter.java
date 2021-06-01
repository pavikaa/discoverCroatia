package com.markopavicic.discovercroatia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {


    private final List<String> commentsList = new ArrayList<>();
    private final List<String> datesList = new ArrayList<>();


    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View cellView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(cellView);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        holder.setData(commentsList.get(position), datesList.get(position));
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public void addData(List<String> commentsList, List<String> datesList) {
        this.commentsList.clear();
        this.datesList.clear();
        this.commentsList.addAll(commentsList);
        this.datesList.addAll(datesList);
        notifyDataSetChanged();
    }
}
