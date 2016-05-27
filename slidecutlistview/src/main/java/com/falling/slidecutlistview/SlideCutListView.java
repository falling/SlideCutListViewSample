package com.falling.slidecutlistview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * Created by falling on 16/5/26.
 */
public class SlideCutListView extends ListView {

    private final int LEFT = 1;
    private final int RIGHT = 2;
    /**
     * 当前滑动的ListView　position
     */
    private int slidePosition;
    /**
     * 手指按下X的坐标
     */
    private int downX;
    /**
     * 手指按下Y的坐标
     */
    private int downY;
    /**
     * 屏幕宽度
     */
    private int screenWidth;
    /**
     * ListView的item
     */
    private View itemView;
    /**
     * 滑动类
     */
    private Scroller scroller;
    private static final int SNAP_VELOCITY = 600;
    /**
     * 速度追踪对象
     */
    private VelocityTracker velocityTracker;
    /**
     * 是否响应滑动，默认为不响应
     */
    private boolean isSlide = false;
    /**
     * 滑动的最小距离
     */
    private int mTouchSlop;
    /**
     * 移除item后的回调接口
     */
    private RemoveListener mRemoveListener;
    /**
     * 用来指示item滑出屏幕的方向,向左或者向右,用一个整形来标记
     */
    private int removeDirection;





    public SlideCutListView(Context context) {
        this(context, null);
    }

    public SlideCutListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideCutListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlideCutListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    /**
     * 初始化屏幕宽带的值、滑动类以及滑动的最小距离
     *
     * @param context
     */
    private void init(Context context) {
        Point size = new Point();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        scroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //当按下的时候，添加速度跟踪器。滑动速度大于一定值的时候，即使没有滑动一定距离也触发滑动删除
                addVelocityTracker(ev);

                // 假如scroller滚动还没有结束，我们直接返回
                if (!scroller.isFinished()) {
                    return super.dispatchTouchEvent(ev);
                }
                //如果不是滚动状态，记下当前的点击位置。并用ListView的pointToPosition函数计算出滑动的item位置。
                downX = (int) ev.getX();
                downY = (int) ev.getY();
                slidePosition = pointToPosition(downX, downY);

                // 无效的position, 不做任何处理
                if (slidePosition == AdapterView.INVALID_POSITION) {
                    return super.dispatchTouchEvent(ev);
                }
                // 获取我们点击的item view
                itemView = getChildAt(slidePosition - getFirstVisiblePosition());
                break;

            case MotionEvent.ACTION_MOVE:
                //如果 1. X的速度大于一定值 2. X滑动的距离大于设定值，并且Y方向的滑动值小于设定值。标记滑动删除。
                if (Math.abs(getScrollVelocity()) > SNAP_VELOCITY
                        || (Math.abs(ev.getX() - downX) > mTouchSlop && Math
                        .abs(ev.getY() - downY) < mTouchSlop)) {
                    isSlide = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                //手指抬起，表示移动结束。移除速度跟踪器。
                removeVelocityTracker();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //如果是有效位置的滑动标记。
        if (isSlide && slidePosition != AdapterView.INVALID_POSITION) {
            addVelocityTracker(ev);
            final int action = ev.getAction();
            int x = (int) ev.getX();
            switch (action) {
                //手指滑动的时候，处理滑动。
                case MotionEvent.ACTION_MOVE:
                    //计算dx滚动的值。
                    int dx = downX - x;
                    downX = x;
                    // item滚动
                    itemView.scrollBy(dx, 0);
                    break;
                //手指抬起的时候，看情况是否触发滑动删除。
                case MotionEvent.ACTION_UP:
                    int velocityX = getScrollVelocity();
                    //根据速度判断
                    if (velocityX > SNAP_VELOCITY) {
                        scrollRight();
                    } else if (velocityX < -SNAP_VELOCITY) {
                        scrollLeft();
                    } else {//根据滑动距离判断。
                        scrollByDistanceX();
                    }
                    removeVelocityTracker();
                    // 手指离开的时候就不响应左右滚动
                    isSlide = false;
                    break;
            }

            return true; // 拖动的时候ListView不滚动
        }

        //否则直接交给ListView来处理onTouchEvent事件
        return super.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        // 调用startScroll的时候scroller.computeScrollOffset()返回true，
        if (scroller.computeScrollOffset()) {
            // 让ListView item根据当前的滚动偏移量进行滚动
            itemView.scrollTo(scroller.getCurrX(), scroller.getCurrY());

            postInvalidate();

            // 滚动动画结束的时候调用回调接口
            if (scroller.isFinished()) {
                if (mRemoveListener == null) {
                    throw new NullPointerException("RemoveListener is null, we should called setRemoveListener()");
                }

                itemView.scrollTo(0, 0);
                mRemoveListener.removeItem(removeDirection, slidePosition);
            }
        }
    }

    /**
     * 根据手指滚动itemView的距离来判断是滚动到开始位置还是向左或者向右滚动
     */
    private void scrollByDistanceX() {
        // 如果向左滚动的距离大于屏幕的三分之一，就让其删除
        if (itemView.getScrollX() >= screenWidth / 3) {
            scrollLeft();
        } else if (itemView.getScrollX() <= -screenWidth / 3) {
            scrollRight();
        } else {
            // 滚回到原始位置
            itemView.scrollTo(0, 0);
        }

    }

    /**
     * 往右滑动，getScrollX()返回的是左边缘的距离，就是以View左边缘为原点到开始滑动的距离，所以向右边滑动为负值
     */
    private void scrollRight() {
        removeDirection = RIGHT;
        final int delta = (screenWidth + itemView.getScrollX());
        // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
        scroller.startScroll(itemView.getScrollX(), 0, -delta, 0,
                Math.abs(delta));
        postInvalidate(); // 刷新itemView
    }

    /**
     * 向左滑动，根据上面我们知道向左滑动为正值
     */
    private void scrollLeft() {
        removeDirection = LEFT;
        final int delta = (screenWidth - itemView.getScrollX());
        // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
        scroller.startScroll(itemView.getScrollX(), 0, delta, 0,
                Math.abs(delta));
        postInvalidate(); // 刷新itemView
    }

    /**
     * 添加速度跟踪器
     *
     * @param ev
     */
    private void addVelocityTracker(MotionEvent ev) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
            velocityTracker.computeCurrentVelocity(1000);
        }
        velocityTracker.addMovement(ev);
    }

    /**
     * 移除速度跟踪器
     */
    private void removeVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker =null;
        }

    }

    public int getScrollVelocity() {
        return (int) velocityTracker.getXVelocity();
    }



    /**
     * 设置滑动删除的回调接口
     *
     * @param removeListener
     */
    public void setRemoveListener(RemoveListener removeListener) {
        mRemoveListener = removeListener;
    }


    /**
     * 滑动删除的回调接口。
     */
    public interface RemoveListener {
        void removeItem(int direction, int position);
    }
}
