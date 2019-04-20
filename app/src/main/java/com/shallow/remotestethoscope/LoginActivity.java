package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.shallow.remotestethoscope.base.DBHelper;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText username;
    private EditText password;
    private Button login_btn;
    private Button register_btn;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        dbHelper = new DBHelper(this, "UserData.db", null, 1);
        username = findViewById(R.id.username_login);
        password = findViewById(R.id.password_login);
        login_btn = findViewById(R.id.login_btn);
        register_btn = findViewById(R.id.register_btn_f);

        login_btn.setOnClickListener(this);
        register_btn.setOnClickListener(this);
        TextChange textChange = new TextChange();
        username.addTextChangedListener(textChange);
        password.addTextChangedListener(textChange);
    }

    public void login(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String password_db;
        String query = "select * from Account where username is ?";
        Cursor cursor = db.rawQuery(query, new String[] {username});
        if (cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                password_db = cursor.getString(cursor.getColumnIndex("password"));
                if (password.equals(password_db)) {
                    SharedPreferences userSetting = getSharedPreferences("setting", 0);
                    SharedPreferences.Editor editor = userSetting.edit();
                    editor.putString("userName", username);
                    editor.apply();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "用户名不存在或用户名错误", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.register_btn_f:
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;
            case R.id.login_btn :
                String mUsername = username.getText().toString();
                String mPassword = password.getText().toString();
                login(mUsername, mPassword);
                break;
        }
    }

    class TextChange implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            int uLength = username.getText().length();
            int pLength = password.getText().length();
            if (uLength > 0 && pLength >0) {
                login_btn.setClickable(true);
                login_btn.setBackgroundResource(R.drawable.ic_fill_button_shape);
            }
        }
    }

}
