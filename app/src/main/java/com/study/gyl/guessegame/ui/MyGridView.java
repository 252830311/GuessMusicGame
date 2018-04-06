package com.study.gyl.guessegame.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.study.gyl.guessegame.model.WordButton;

import java.util.ArrayList;


public class MyGridView extends GridView {
    private ArrayList<WordButton> mArrayList = new ArrayList<>();
    private MyGridAdapter mAdapter;

    public MyGridView(Context context) {
        this(context, null);
    }

    public MyGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAdapter = new MyGridAdapter();
        this.setAdapter(mAdapter);
    }


    class MyGridAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mArrayList.size();
        }

        @Override
        public Object getItem(int i) {
            return mArrayList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {

            } else {

            }


            return null;
        }
    }
}

