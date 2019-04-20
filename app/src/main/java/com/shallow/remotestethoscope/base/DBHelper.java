package com.shallow.remotestethoscope.base;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String CREATE_ACCOUNT = "create table Account (" +
            "id integer primary key autoincrement," +
            "username text," +
            "password text)";

    private static final String CREATE_FILE = "create table AudioFile (" +
            "id integer primary key autoincrement," +
            "mp3_file_name text," +
            "mp3_file_time text," +
            "mp3_file_duration text," +
            "username text)";


    private Context mContext;

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ACCOUNT);
        db.execSQL(CREATE_FILE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
