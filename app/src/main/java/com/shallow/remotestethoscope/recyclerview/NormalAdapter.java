package com.shallow.remotestethoscope.recyclerview;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shallow.remotestethoscope.R;
import com.shallow.remotestethoscope.base.DBHelper;
import com.shallow.remotestethoscope.base.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NormalAdapter extends RecyclerView.Adapter<NormalAdapter.VH> {
    public static final int NORMAL = 1;
    public static final int OPERATE = 2;
    public static final int SELECT_ALL = 3;

    public static final boolean NOT_SELECTED = false;
    public static final boolean SELECTED = true;

    private ArrayList<Integer> positions = new ArrayList<>();

    private int file_status = NORMAL;

    private ArrayList<ObjectModel> mDatas;
    private Context mContext;
    private DBHelper mDBhelper;

    private RecyclerView recyclerView;
    private LinearLayout operateMenu;

    public NormalAdapter(ArrayList<ObjectModel> data, Context context, DBHelper dbHelper) {
        this.mDatas = data;
        this.mContext = context;
        this.mDBhelper = dbHelper;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, final int position) {
        final ObjectModel model =mDatas.get(position);
        if (file_status == NORMAL) {
            holder.fileName.setText(model.mp3Name);
            holder.fileDetails.setText(model.mp3Detail);
            holder.fileIcon.setImageResource(model.mp3Img);
            model.setMp3Status(NOT_SELECTED);
        } else if (file_status == OPERATE){
            holder.fileName.setText(model.mp3Name);
            holder.fileDetails.setText(model.mp3Detail);
            holder.fileIcon.setImageResource(R.drawable.ic_action_checkbox_gray);
            model.setMp3Status(NOT_SELECTED);
        } else if (file_status == SELECT_ALL) {
            holder.fileName.setText(model.mp3Name);
            holder.fileDetails.setText(model.mp3Detail);
            holder.fileIcon.setImageResource(R.drawable.ic_action_checkbox);
            model.setMp3Status(SELECTED);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if (file_status == NORMAL) {

                } else {
                    if (!model.isMp3Status()) {
                        holder.fileIcon.setImageResource(R.drawable.ic_action_checkbox);
                        positions.add(position);
                        model.setMp3Status(SELECTED);
                        mDatas.set(position, model);

                    } else {
                        holder.fileIcon.setImageResource(R.drawable.ic_action_checkbox_gray);
                        positions.remove((Integer) position);
                        model.setMp3Status(NOT_SELECTED);
                        mDatas.set(position, model);

                    }
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                file_status = OPERATE;
                notifyDataSetChanged();
                operateMenu.setVisibility(LinearLayout.VISIBLE);
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }



    static class VH extends RecyclerView.ViewHolder {

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

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setOperateMenu(LinearLayout operateMenu) {
        this.operateMenu = operateMenu;
    }

    public void setFile_status(int file_status) {
        this.file_status = file_status;
    }

    public void clearPositions() {
        positions.clear();
    }

    public void deleteFile() {
        if (positions.size() == 0) {
            Toast.makeText(mContext, "请选择需要删除的文件", Toast.LENGTH_SHORT).show();
        } else {
            Collections.sort(positions, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if (o1 > o2){
                        return -1;
                    } else if (o1 < o2) {
                        return 1;
                    }
                    return 0;
                }
            });
            SQLiteDatabase db = mDBhelper.getWritableDatabase();
            for (Integer pos : positions) {
                //数据库删除
                ObjectModel ob = mDatas.get(pos);
                db.beginTransaction();
                try {
                    db.delete("AudioFile", "mp3_file_name = ?", new String[] {ob.getMp3Name()});
                    db.setTransactionSuccessful();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    db.endTransaction();
                }

                //文件删除
                String rootPath = FileUtils.getAppPath();
                String filePath = rootPath + File.separator + ob.getMp3Name() + ".mp3";
                FileUtils.deleteFile(filePath);

                //数据集删除
                mDatas.remove((int)pos);

            }
            positions.clear();
            file_status = NORMAL;
            notifyDataSetChanged();
            operateMenu.setVisibility(LinearLayout.INVISIBLE);
        }
    }

    public void renameFile() {
        if (positions.size() > 1) {
            Toast.makeText(mContext, "不能同时重命名多个文件", Toast.LENGTH_SHORT).show();
        } else if (positions.size() == 0) {
            Toast.makeText(mContext, "请选择需要重命名的文件", Toast.LENGTH_SHORT).show();
        } else {
            positions.size();
            final ObjectModel tmpOB = mDatas.get(positions.get(0));

            final EditText name = new EditText(mContext);
            AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle("输入文件名")
                    .setIcon(R.mipmap.ic_action_edit_file)
                    .setView(name)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newName = name.getText().toString();
                            if (newName.equals("")) {
                                Toast.makeText(mContext, "文件名不能为空", Toast.LENGTH_SHORT).show();
                            } else {

                                for (ObjectModel obj : mDatas) {
                                    if (obj.getMp3Name().equals(newName)) {
                                        Toast.makeText(mContext, "文件名已存在", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }

                                String oldName = tmpOB.getMp3Name();
                                String rootPath = FileUtils.getAppPath();
                                String oldFilePath = rootPath + File.separator + oldName + ".mp3";
                                String newFilePath = rootPath + File.separator + oldName + ".mp3";

                                File oldFile = new File(oldFilePath);
                                File newFile = new File(newFilePath);

                                //文件重命名
                                oldFile.renameTo(newFile);
                                FileUtils.deleteFile(oldFilePath);

                                //数据库修改
                                SQLiteDatabase db = mDBhelper.getWritableDatabase();
                                ContentValues values = new ContentValues();
                                values.put("mp3_file_name", newName);
                                db.beginTransaction();
                                try {
                                    db.update("AudioFile", values, "mp3_file_name = ?", new String[] {oldName});
                                    db.setTransactionSuccessful();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    db.endTransaction();
                                }

                                //数据集修改
                                tmpOB.setMp3Name(newName);
                                mDatas.set(positions.get(0), tmpOB);
                                file_status = NORMAL;
                                notifyDataSetChanged();
                                operateMenu.setVisibility(LinearLayout.INVISIBLE);

                            }
                        }
                    })
                    .setNegativeButton("取消",null)
                    .show();
            Window window = dialog.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    public void selectAllFile() {
        file_status = SELECT_ALL;
        for (int i = 0; i < mDatas.size(); i++) {
            positions.add(i);
        }
        notifyDataSetChanged();
    }

}
