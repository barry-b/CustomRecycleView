package com.barry.customrecycleview;

import android.view.View;

import java.util.Stack;

/**
 * 回收池
 */
public class Recycler {

    //stack数组，要用多个stack来缓存不同类型的item
    private Stack<View>[] views;

    /**
     * 回收池中缓存的item有多种类型，每种类型的item用一个栈来保存
     * @param typeCount
     */
    public Recycler(int typeCount) {
        views = new Stack[typeCount];
        for (int i = 0; i < typeCount; i++) {
            views[i] = new Stack<>();
        }
    }

    /**
     * 获取缓存的Item
     * @param type  Item类型
     * @return
     */
    public View getRecyclerView(int type){
        try{
            return views[type].pop();
        }catch (Exception e){
            return null;
        }
    }

    public void addRecyclerView(View view,int type){
        views[type].push(view);
    }
}
