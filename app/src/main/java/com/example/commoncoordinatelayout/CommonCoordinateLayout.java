package com.example.commoncoordinatelayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by 10732 on 2019/3/1.
 */

public class CommonCoordinateLayout extends LinearLayout {

    private Context context;
    private Scroller mScroller;
    private VelocityTracker velocityTracker;

    public CommonCoordinateLayout(Context context) {
        super(context);
        init(context);
    }

    public CommonCoordinateLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CommonCoordinateLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        setOrientation(VERTICAL);
        mScroller = new Scroller(context, null);
        SCROLL_DELAY_DISTANCE = 30;
        ADSORPTION_DISTANCE = 80;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        /*
         * HelpLayout的Clone事件
         * */
        MotionEvent eventHelpLayoutClone = MotionEvent.obtain(event);
        float cloneHelpLayoutY = event.getY() + getScrollY();
        eventHelpLayoutClone.setLocation(event.getX(), cloneHelpLayoutY);

        /*
         * RecyclerView的Clone事件
         * */
        MotionEvent eventClone = MotionEvent.obtain(event);
        float cloneY = event.getY() - recyclerView.getTop() + getScrollY();
        eventClone.setLocation(eventClone.getRawX(), cloneY);

        float currentY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                recyclerView.dispatchTouchEvent(eventClone);
                helpLayout.dispatchTouchEvent(eventHelpLayoutClone);
                lastY = startY = currentY;
                mScroller.forceFinished(true);
                isFling = false;
                isSelfConsumer = false;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float dY = lastY - currentY;

                /*
                 * 如果没有在滚动就将事件传到 HelpLayout 里面
                 * */
                if (!isSelfConsumer) {
                    helpLayout.dispatchTouchEvent(eventHelpLayoutClone);
                }

                if (dY > 0) {
                    /*
                     * 向上滑动时
                     * 当向上滑动超过MaxScrollY之后, 交给RecyclerView处理
                     * 否则自己处理
                     * */
                    if (getScrollY() >= maxScrollY) {
                        recyclerView.dispatchTouchEvent(eventClone);
                        isRecyclerViewMoving = true;
                    } else {
                        dealBySelf(dY, Math.abs(currentY - startY));
                        isRecyclerViewMoving = false;
                    }
                } else {
                    /*
                     * 向下滑动时
                     * 当RecyclerView没有滑动到顶部时, 交给RecyclerView处理
                     * 否则自己处理
                     * */
                    if (!isScrollTop(recyclerView)) {
                        recyclerView.dispatchTouchEvent(eventClone);
                        isRecyclerViewMoving = true;
                    } else {
                        dealBySelf(dY, Math.abs(currentY - startY));
                        isRecyclerViewMoving = false;
                    }
                }
                lastY = currentY;
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isTouchedOnRecyclerView(currentY)) {
                    /*
                     * 在移动Parent的时候,手指移动小于10像素  或者  现在是RecyclerView接管事件的时候
                     * 将 UP 事件传递给 RecyclerView, 认为是点击
                     * 否则将事件改成 CANCEL,为了去掉 item_press_selector
                     * */
                    if ((Math.abs(startY - lastY) < 10 || isRecyclerViewMoving)) {
                        recyclerView.dispatchTouchEvent(eventClone);
                    } else {
                        eventClone.setAction(MotionEvent.ACTION_CANCEL);
                        recyclerView.dispatchTouchEvent(eventClone);
                    }

                    performClick();
                }

                /*
                 * 如果没有在滚动就将事件传到 HelpLayout 里面
                 * */
                if (!isSelfConsumer) {
                    helpLayout.dispatchTouchEvent(eventHelpLayoutClone);
                }

                /*
                 * 惯性处理 (不是在操作Banner的时候才进行惯性滑动处理)
                 * */
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float yVelocity = velocityTracker.getYVelocity();

                    velocityTracker.recycle();
                    velocityTracker = null;

                    /*
                     * 如果 <=0 并且不足够刷新, 回到 0 (下滑惯性)
                     * 如果 >=MaxScroll ,回到MaxScroll (上滑惯性)
                     * ADSORPTION_DISTANCE : 一个吸附功能, 在 maxScrollY 附近的距离被吸附
                     * */

                    if (getScrollY() <= minScrollY) {
                        isFling = true;
                        mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + minScrollY);
                    } else if (getScrollY() >= maxScrollY - ADSORPTION_DISTANCE) {
                        mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + maxScrollY);
                        isFling = true;
                    } else {
                        mScroller.fling(0, getScrollY(), 0, (int) -yVelocity, 0, 0, minScrollY - 1000, maxScrollY + 1000);
                        isFling = true;
                    }
                    postInvalidate();
                }
                break;
        }

        eventClone.recycle();
        eventHelpLayoutClone.recycle();

        return true;
    }

    private void dealBySelf(float dY, float moveY) {

        /*
         * 做一个小的延时, 当手指垂直发生一定的距离时再开始滑动
         * 如果 ViewPager 也就是Banner开始滚动了, 就不要在垂直滚动了
         * */
        if (!isSelfConsumer && moveY < SCROLL_DELAY_DISTANCE) return;

        if (getScrollY() + dY > maxScrollY) {
            scrollTo(0, maxScrollY);
        } else if (getScrollY() + dY < minScrollY) {
            scrollBy(0, (int) (dY / 1.5f));
        } else {
            scrollBy(0, (int) dY);
        }
    }

    private boolean isTouchedOnRecyclerView(float currentY) {
        return currentY >= helpLayout.getMeasuredHeight() - getScrollY();
    }

    private boolean isScrollTop(View recyclerView) {

        if (recyclerView instanceof RecyclerView) {
            if (((RecyclerView) recyclerView).getAdapter() == null || ((RecyclerView) recyclerView).getAdapter().getItemCount() == 0)
                return true;

            if (((RecyclerView) recyclerView).getLayoutManager() instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) ((RecyclerView) recyclerView).getLayoutManager();
                int firstCompletelyVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                if (firstCompletelyVisibleItemPosition == 0) {
                    return true;
                }
            }
            if (((RecyclerView) recyclerView).getLayoutManager() instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) ((RecyclerView) recyclerView).getLayoutManager();
                int[] aa = staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(null);
                if (aa[0] == 0) {
                    return true;
                }
            }
        } else if (recyclerView instanceof ScrollView) {
            recyclerView = (ScrollView) recyclerView;
            return recyclerView.getScrollY() == 0;
        }

        return false;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            int nextY = mScroller.getCurrY();
            scrollTo(0, nextY);
            if (nextY > maxScrollY + OVER_SCROLL_LENGTH) {
                //向上 Over Scroll
                if (isFling) {
                    mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + maxScrollY);
                    isFling = false;
                }
            } else if (nextY < minScrollY - OVER_SCROLL_LENGTH) {
                //向下 Over Scroll
                if (isFling) {
                    mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
                    isFling = false;
                }
            }
            postInvalidate();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        /*
         * 监听滚动距离, 更新刷新区域内容
         * */
        isSelfConsumer = true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean isFling;
    private boolean isRecyclerViewMoving;
    private int statusBarHeight;
    private float startY;
    private float lastY;
    private int maxScrollY, minScrollY;
    private View recyclerView;
    private LinearLayout helpLayout;
    private final int OVER_SCROLL_LENGTH = 0;
    private boolean isSelfConsumer;
    private int ADSORPTION_DISTANCE;
    private int SCROLL_DELAY_DISTANCE;

    public void setMaxScroll(int maxScrollY) {
        this.maxScrollY = maxScrollY;
    }

    public void setMinScroll(int minScroll) {
        this.minScrollY = minScroll;
    }

    public void setRecyclerView(View recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setHelpLayout(LinearLayout helpLayout) {
        this.helpLayout = helpLayout;
    }

    public void setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }

}
