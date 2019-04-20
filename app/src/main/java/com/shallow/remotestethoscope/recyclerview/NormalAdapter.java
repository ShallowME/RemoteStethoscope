package com.shallow.remotestethoscope.recyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shallow.remotestethoscope.R;

import java.util.List;

public class NormalAdapter extends RecyclerView.Adapter<NormalAdapter.VH> {

    private List<ObjectModel> mDatas;

    public NormalAdapter(List<ObjectModel> data) {
        this.mDatas = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ObjectModel model =mDatas.get(position);
        holder.fileName.setText(model.mp3Name);
        holder.fileDetails.setText(model.mp3Detail);
        holder.fileIcon.setImageResource(model.mp3Img);
        holder.itemView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public static class VH extends RecyclerView.ViewHolder {

        public final TextView fileName;
        public final TextView fileDetails;
        public final ImageView fileIcon;

        public VH(@NonNull View itemView) {
            super(itemView);
            this.fileName = itemView.findViewById(R.id.mp3Name);
            this.fileDetails = itemView.findViewById(R.id.mp3Detail);
            this.fileIcon = itemView.findViewById(R.id.mp3Img);
        }
    }

}
