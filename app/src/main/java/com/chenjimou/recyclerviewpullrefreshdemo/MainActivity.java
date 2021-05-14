package com.chenjimou.recyclerviewpullrefreshdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chenjimou.recyclerviewpullrefreshdemo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private ActivityMainBinding mBinding;
    private final List<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        init();
        initListener();
    }

    private void initListener()
    {

    }

    private void init()
    {
        for (int i = 0; i < 100; i++)
        {
            list.add("item"+(i+1));
        }

        setSupportActionBar(mBinding.toolbar);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
        mBinding.recyclerView.setAdapter(new MyAdapter());
    }

    class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            RecyclerView.ViewHolder viewHolder;
            viewHolder = new ItemViewHolder(LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.item, parent, false));
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            ((ItemViewHolder) holder).tv.setText(list.get(position));
        }

        @Override
        public int getItemCount()
        {
            return list.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder
        {
            private final TextView tv;
            public ItemViewHolder(@NonNull View itemView)
            {
                super(itemView);
                tv = itemView.findViewById(R.id.textView);
            }
        }
    }
}