package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FunctionChooseActivity extends AppCompatActivity implements View.OnClickListener {

    private Button tone_choose_btn;
    private Button emg_choose_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function_choose);

        tone_choose_btn = findViewById(R.id.tone_choose);
        emg_choose_btn = findViewById(R.id.emg_choose);
        tone_choose_btn.setOnClickListener(this);
        emg_choose_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.tone_choose:
                Intent mainIntent = new Intent(FunctionChooseActivity.this, MainActivity.class);
                startActivity(mainIntent);
                break;

            case R.id.emg_choose:
                Intent EmgDisplayIntent = new Intent(FunctionChooseActivity.this, BlueToothActivity.class);
                startActivity(EmgDisplayIntent);
                break;
        }
    }
}
