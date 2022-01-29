package com.barry.customrecycleview;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRv;

    int count = 50000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mRv = findViewById(R.id.rv);
        mRv.setAdapter(new MyAdapter(this));
    }

    class MyAdapter implements RecyclerView.Adapter{

        LayoutInflater mInflater;
        private int height;

        public MyAdapter(Context context) {
            height = context.getResources().getDimensionPixelSize(R.dimen.item_height);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View onCreateViewHolder(int position, View convertView, ViewGroup parent) {
            if(position % 2 == 0){
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_test,parent,false);
                }
                TextView tv = convertView.findViewById(R.id.tv);
                tv.setText("第" + position + "行");
            }else{
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_test2,parent,false);
                }
            }
            return convertView;
        }

        @Override
        public View onBindViewHolder(int position, View convertView, ViewGroup parent) {
            /*if(position % 2 == 0){
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_test,parent,false);
                }

            }else{
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_test2,parent,false);
                }
            }*/
            TextView tv = convertView.findViewById(R.id.tv);
            tv.setText("第" + position + "行");
            return convertView;

        }

        @Override
        public int getItemViewType(int row) {
            if(row % 2 == 0){
                return 0;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getHeight(int index) {
            return height;
        }
    }
}