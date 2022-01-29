package com.barry.customrecycleview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 手写RV
 * 需要重写的方法：
 *      1. onMeasure
 *      2. onLayout
 *      3. onInterceptHoverEvent
 *      4. onInterceptTouchEvent
 *      5. onTouchEvent
 *      6. scrolledBy
 */
public class RecyclerView extends ViewGroup {

    private static final String TAG = RecyclerView.class.getSimpleName();
    //用来判断onLayout中的逻辑是否执行
    private boolean needRelayout;
    //缓存存在屏幕上的view（与回收池无关）
    private List<View> viewList;

    private Adapter mAdapter;

    //回收池
    private Recycler mRecycler;

    //记录当前行数
    private int rowCount;
    //每一行高度
    private int[] heights;
    //rv的宽高
    private int width,height;
    //
    private int touchSlop;
    //当前滑动的y值
    private int curY;
    //偏移距离(第一个可见item的左上顶点距离屏幕左上角的距离)
    //前一个点（item左上角最开始的位置） - 后一个点（滑动后item左上角的位置）
    private int scrollY;
    //滑到第几行（表示屏幕上第一个可见item在实际数据中的索引）
    private int firstRow;

    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 初始化
     * @param context
     */
    private void init(Context context) {
        viewList = new ArrayList<>();
        needRelayout = true;
        //获取最小滑动距离
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        touchSlop = viewConfiguration.getScaledTouchSlop();
    }

    /**
     * 设置适配器
     * @param adapter
     */
    public void setAdapter(Adapter adapter){
        if (adapter != null) {
            needRelayout = true;
            mAdapter = adapter;
            mRecycler = new Recycler(mAdapter.getViewTypeCount());
        }
    }

    //region rv的滑动逻辑实现

    /**
     * 滑动拦截 点击不拦截
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                curY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                //获取滑动的距离
                int moveY = Math.abs(curY - (int) ev.getRawY());
                if (moveY > touchSlop) {
                    //如果大于最小滑动距离，则认为发生了滑动
                    intercept = true;
                    //解决子View也有滑动事件造成的冲突
                    /*for(View child : getChildren()){
                        if(child instanceof ScrollView){
                            //在判断用户此时触摸的位置是否在该控件边界内
                            //如果在则不拦截（不拦截就是让子View去消费）
                            if(ev.getX() > child.getLeft() && ev.getX() < child.getRight()){
                                return false;
                            }
                        }
                    }*/
                }
        }
        return intercept;
    }

    /**
     * 消费事件
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                int y = (int) event.getRawY();
                int diff = curY - y;
                //滑动，直接调用滑动的是一块固定的画布，所以要重写scrollBy
                scrollBy(0,diff);
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 需要重写scrollBy，因为默认的scrollBy滑动的是一块固定的画布
     * 我们需要的是不断地去更新item的位置，重新去进行摆放
     * @param x
     * @param diff
     */
    @Override
    public void scrollBy(int x, int diff) {
        scrollY += diff;

        //修正
        scrollY = scrollBounds(scrollY,firstRow,heights,height);
        if(scrollY > 0){
            //向上划
            while(heights[firstRow] < scrollY){
                //如果滑动的距离大于第一个可见item的高度
                if (!viewList.isEmpty()) {
                    //移除最顶上的view
                    removeView(viewList.remove(0));
                }
                scrollY -= heights[firstRow];
                firstRow++;
            }

            while(getFilledHeight() < height){
                //如果当前填充屏幕的item总高度小于rv高度
                //开始添加view
                int size = viewList.size();
                int dataIndex = firstRow + size;
                View view = obtain(dataIndex, width, heights[dataIndex]);
                viewList.add(view);
            }
        }else{
            //向下滑
            while (!viewList.isEmpty() && getFilledHeight() - heights[firstRow + viewList.size() - 1] < scrollY) {
                View view = viewList.remove(viewList.size() - 1);
                removeView(view);
            }
            while(scrollY < 0){
                firstRow--;
                View view = obtain(firstRow, width, heights[firstRow]);
                viewList.add(0,view);
                scrollY += heights[firstRow + 1];
            }
        }
        repositionViews();
    }

    private int scrollBounds(int scrollY, int firstRow, int[] heights, int viewSize) {

        if (scrollY == 0) {

        }else if(scrollY < 0){
            //修整下滑临界值
             scrollY = Math.max(scrollY, -sumArray(heights, 0, firstRow));
        }else{
            scrollY = Math.min(scrollY,Math.max(0, sumArray(heights, firstRow, heights.length - viewSize)));
        }
        return scrollY;
    }

    /**
     * 获取到当前填充屏幕上的item的总高度
     * @return 原先填充屏幕的item总高度减去第一个可见item的偏移距离
     */
    private int getFilledHeight() {
        return sumArray(heights,firstRow,viewList.size()) - scrollY;
    }

    private int sumArray(int[] heights, int firstIndex, int count) {
        int sum = 0;
        count += firstIndex;
        for (int i = firstIndex; i < count; i++) {
            sum += heights[i];
        }
        return sum;
    }

    /**
     * 滑动过程中重新摆放View
     */
    private void repositionViews() {
        int left,top,right,bottom,i;
        top = -scrollY;
        i = firstRow;
        for (View view : viewList) {
            bottom = top + heights[i++];
            view.layout(0,top,width,bottom);
            top = bottom;
        }
    }

    //endregion


    //region item布局实现
    /**
     * 每次重新摆放子控件的时候会调用onLayout
     * onLayout会遍历所有的childView（耗时）
     * 所以我们需要在恰当的时候执行onLayout中的业务逻辑，其余时候不会执行
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //当父布局发生变化是，changed为true
        if(needRelayout || changed){
            //重新摆放子控件
            needRelayout = false;
            //初始化
            viewList.clear();
            removeAllViews();
            //RV只做滑动的处理，不做具体的UI，UI的显示是交给adapter的。
            if(mAdapter != null){
                rowCount = mAdapter.getCount();
                heights = new int[rowCount];
                //获取高度
                for (int j = 0; j < rowCount; j++) {
                    //依赖这个方法测量item的高度
                    heights[j] += mAdapter.getHeight(j);
                }

                width = r - l;
                height = b - t;
                int top = 0,bottom = 0;
                //填充第一屏,直到bottom（此时item的底部在屏幕中的位置） >= 屏幕的高度
                for (int j = 0; j < rowCount && top < height; j++) {
                    bottom = top + heights[j];
                    //实例化布局，传递索引以及四个坐标值
                    View view = makeAndSetup(j,0,top,width,bottom);
                    viewList.add(view);
                    top = bottom;
                }
            }
        }
    }

    //将Item布局在RV控件中进行摆放
    private View makeAndSetup(int index, int left, int top, int right, int bottom) {
        View view = obtain(index,right - left,bottom - top);
        //通过obtain()方法获取的view是没有宽高的，需要调用layout方法进行摆放
        view.layout(left,top,right,bottom);
        return view;
    }

    /**
     * 获取到Item布局
     * @param index  位置
     * @param width
     * @param height
     * @return
     */
    private View obtain(int index, int width, int height) {
        //先查看回收池中是否有可以复用的item
        int type = mAdapter.getItemViewType(index);
        View recView = mRecycler.getRecyclerView(type);

        View view = null;
        if (recView == null) {
            view = mAdapter.onCreateViewHolder(index,null,this);
            if (view == null) {
                throw new RuntimeException("onCreateViewHolder 必须初始化!");
            }
        }else{
            //如果从回收池里拿到了可以复用的item，则刷新该item上的数据
            view = mAdapter.onBindViewHolder(index,recView,this);
        }
        if(view == null) throw new RuntimeException("convertView can't be null!");
        //因为回收池的填充和移除需要成对出现，所以需要给item设置TAG
        //当回收池中有view被移除时，就会有屏幕上划出去的view填充进去
        view.setTag(R.id.tag_type_view,type);

        //测量
        view.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY) );
        addView(view,0);
        return view;
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        int type = (int) view.getTag(R.id.tag_type_view);
        mRecycler.addRecyclerView(view,type);
    }

    //endregion

    /**
     * Adapter内部类
     */
    interface Adapter{
        View onCreateViewHolder(int position, View convertView, ViewGroup parent);
        View onBindViewHolder(int position, View convertView, ViewGroup parent);
        //Item类型
        int getItemViewType(int row);
        //Item类型数量
        int getViewTypeCount();
        
        int getCount();
        //这里的高度写死
        int getHeight(int index);
    }
}
