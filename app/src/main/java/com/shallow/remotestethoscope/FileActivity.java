package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.shallow.remotestethoscope.base.DBHelper;
import com.shallow.remotestethoscope.recyclerview.NormalAdapter;
import com.shallow.remotestethoscope.recyclerview.ObjectModel;

import java.util.ArrayList;

public class FileActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView mRecyclerView;
    private NormalAdapter mNoHeaderAdapter;
    private DividerItemDecoration mDecoration;
    private RecyclerView.LayoutManager mLayoutManager;

    private ImageButton cancel_operate_btn;
    private ImageButton delete_file_btn;
    private ImageButton rename_file_btn;
    private ImageButton select_all_btn;

    LinearLayout operateMenu;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        Toolbar toolbar = findViewById(R.id.toolbar_file);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FileActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        dbHelper = new DBHelper(this, "UserData.db", null, 1);

        cancel_operate_btn = findViewById(R.id.cancel_operate_button);
        delete_file_btn = findViewById(R.id.delete_file_button);
        rename_file_btn = findViewById(R.id.rename_file_button);
        select_all_btn = findViewById(R.id.select_all_button);
        cancel_operate_btn.setOnClickListener(this);
        delete_file_btn.setOnClickListener(this);
        rename_file_btn.setOnClickListener(this);
        select_all_btn.setOnClickListener(this);

        mRecyclerView = findViewById(R.id.fileList);
        mDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mDecoration);
        mNoHeaderAdapter = new NormalAdapter(initData(), FileActivity.this, dbHelper);
        mRecyclerView.setAdapter(mNoHeaderAdapter);
        mNoHeaderAdapter.setRecyclerView(mRecyclerView);
        operateMenu = findViewById(R.id.fileOperateMenu);
        mNoHeaderAdapter.setOperateMenu(operateMenu);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_operate_button :
                operateMenu.setVisibility(LinearLayout.INVISIBLE);
                mNoHeaderAdapter.clearPositions();
                mNoHeaderAdapter.setFile_status(NormalAdapter.NORMAL);
                mNoHeaderAdapter.notifyDataSetChanged();
                break;
            case R.id.delete_file_button :
                mNoHeaderAdapter.deleteFile();
                break;

            case R.id.rename_file_button :
                mNoHeaderAdapter.renameFile();
                break;

            case R.id.select_all_button :
                mNoHeaderAdapter.selectAllFile();
                break;
        }
    }


    public ArrayList<ObjectModel> initData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SharedPreferences userSetting = getSharedPreferences("setting", 0);
        String username = userSetting.getString("username", "");
        Cursor cursor = db.rawQuery("select * from AudioFile where username is ?", new String[] {username});
        ArrayList<ObjectModel> modelList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                ObjectModel ob = new ObjectModel();
                ob.setMp3Name(cursor.getString(cursor.getColumnIndex("mp3_file_name")));
                String detail = cursor.getString(cursor.getColumnIndex("mp3_file_time")) +
                        "  |  " + cursor.getString(cursor.getColumnIndex("mp3_file_duration"));
                ob.setMp3Detail(detail);
                ob.setMp3Img(R.drawable.ic_action_play);
                ob.setMp3Status(NormalAdapter.NOT_SELECTED);
                modelList.add(ob);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return modelList;
    }

}
