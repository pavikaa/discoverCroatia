package com.markopavicic.myapplication;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

public class ViewHolder extends RecyclerView.ViewHolder{
    private final TextView tvComment,tvDate;
    public ViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
        tvComment = itemView.findViewById(R.id.tvComment);
        tvDate = itemView.findViewById(R.id.tvDate);
    }
    public void setData(String comment, String date)
    {
        tvComment.setText(comment);
        tvDate.setText(date);
    }
}
