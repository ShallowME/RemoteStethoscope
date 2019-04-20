package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.shallow.remotestethoscope.base.DBHelper;

public class RegisterActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = findViewById(R.id.toolbar_register);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        dbHelper = new DBHelper(this, "UserData.db", null, 1);
        Button register_btn = findViewById(R.id.register_btn_s);
        username = findViewById(R.id.username_register);
        password = findViewById(R.id.password_register);
        register_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int uLength = username.getText().length();
                int pLength = password.getText().length();

                if (uLength > 0 && pLength > 0) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    String query = "select * from Account where username is ?";
                    Cursor cursor = db.rawQuery(query, new String[] {username.getText().toString()});
                    if (cursor.getCount() > 0){
                        Toast.makeText(RegisterActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                    } else {
                        db.beginTransaction();
                        try {
                            ContentValues values = new ContentValues();
                            values.put("username", username.getText().toString());
                            values.put("password", password.getText().toString());
                            db.insert("Account", null, values);
                            values.clear();
                            db.setTransactionSuccessful();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            db.endTransaction();
                        }
                        Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                    cursor.close();
                } else {
                    if (uLength == 0 && pLength > 0) {
                        Toast.makeText(RegisterActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                    } else if (uLength > 0 && pLength == 0) {
                        Toast.makeText(RegisterActivity.this, "密码不能为空", Toast.LENGTH_SHORT).show();
                    } else if (uLength == 0 && pLength == 0) {
                        Toast.makeText(RegisterActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
