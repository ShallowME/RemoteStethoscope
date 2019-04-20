package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.shallow.remotestethoscope.R;
import com.shallow.remotestethoscope.recyclerview.NormalAdapter;
import com.shallow.remotestethoscope.recyclerview.ObjectModel;

import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ArrayList<ObjectModel> mData;
//    private NormalAdapterWrapper mAdapter;
    private NormalAdapter mNoHeaderAdapter;
    private DividerItemDecoration mDecoration;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        mRecyclerView = findViewById(R.id.fileList);
        mDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mDecoration);


        mNoHeaderAdapter = new NormalAdapter(mData);

    }
}
