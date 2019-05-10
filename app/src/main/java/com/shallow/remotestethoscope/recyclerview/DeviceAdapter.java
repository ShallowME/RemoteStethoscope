package com.shallow.remotestethoscope.recyclerview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shallow.remotestethoscope.EmgDisplayActivity;
import com.shallow.remotestethoscope.R;

import java.util.ArrayList;

import static com.shallow.remotestethoscope.EmgDisplayActivity.EXTRAS_DEVICE_ADDRESS;
import static com.shallow.remotestethoscope.EmgDisplayActivity.EXTRAS_DEVICE_NAME;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.VH> {

    private ArrayList<DeviceModel> mDatas;

    private RecyclerView recyclerView;

    private Context mContext;

    private boolean isScanning = false;

    public DeviceAdapter(Context context, ArrayList<DeviceModel> datas) {
        this.mDatas = datas;
        this.mContext = context;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final DeviceModel dm = mDatas.get(position);
        holder.blueName.setText(dm.getDeviceName());
        holder.blueMAC.setText(dm.getDeviceMAC());
        holder.blueRssi.setText(dm.getDeviceRssi());

        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, EmgDisplayActivity.class);
                intent.putExtra(EXTRAS_DEVICE_ADDRESS, dm.getDeviceMAC());
                mContext.startActivity(intent);
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

    public void setDatas(ArrayList<DeviceModel> datas) {
        this.mDatas = datas;
    }

    public void setIsScanning(boolean isScanning) {
        this.isScanning = isScanning;
    }

    static class VH extends RecyclerView.ViewHolder {

        final TextView blueName;
        final TextView blueMAC;
        final TextView blueRssi;

        private VH(@NonNull View itemView) {
            super(itemView);
            this.blueName = itemView.findViewById(R.id.deviceName);
            this.blueMAC = itemView.findViewById(R.id.mac);
            this.blueRssi = itemView.findViewById(R.id.rssi);
        }

    }

}
