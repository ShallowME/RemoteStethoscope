package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.shallow.remotestethoscope.base.DBHelper;
import com.shallow.remotestethoscope.recyclerview.NormalAdapter;
import com.shallow.remotestethoscope.recyclerview.ObjectModel;

import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private NormalAdapter mNoHeaderAdapter;
    private DividerItemDecoration mDecoration;
    private RecyclerView.LayoutManager mLayoutManager;
    //    private NormalAdapterWrapper mAdapter;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        dbHelper = new DBHelper(this, "UserData.db", null, 1);

        mRecyclerView = findViewById(R.id.fileList);
        mDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mDecoration);
        mNoHeaderAdapter = new NormalAdapter(initData());
        mRecyclerView.setAdapter(mNoHeaderAdapter);
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
                ob.setMp3Img(R.drawable.ic_action_playlist);
                modelList.add(ob);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return modelList;
    }

}
