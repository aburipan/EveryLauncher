package jp.aburipan.everylauncher.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NativePager extends FrameLayout {
    private static final int PAGE_DRAG_START_THRESHOLD_DP = UiDimensions.SPACING_12_DP;
    private static final int PAGE_CHANGE_THRESHOLD_DP = UiDimensions.SPACING_48_DP;
    private static final int PAGE_SETTLE_DURATION_MILLIS = 240;
    private static final int PAGE_FLING_VELOCITY_DP_PER_SECOND = 600;
    private static final float DP_ROUNDING_OFFSET = 0.5f;
    private static final float PAGE_RESISTANCE = 0.25f;
    private static final float PAGE_TRANSFORM_POSITION_LIMIT = 1.0f;
    private List<View> pages = Collections.emptyList();
    private PageTransformer transformer;
    private OnPageSelectedListener listener;
    private ValueAnimator settleAnimator;
    private VelocityTracker velocityTracker;
    private int currentItem;
    private float downX;
    private float downY;
    private float dragOffsetX;
    private boolean horizontalGesture;

    public NativePager(Context context) {
        super(context);
        setClipChildren(false);
    }

    public void setPages(List<View> newPages) {
        removeAllViews();
        pages = new ArrayList<>(newPages);
        currentItem = Math.min(currentItem, Math.max(0, pages.size() - 1));
        for (View page : pages) {
            addView(page, new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
        renderPages();
    }

    public void setPageTransformer(PageTransformer pageTransformer) {
        transformer = pageTransformer;
        renderPages();
    }

    public void setOnPageSelectedListener(OnPageSelectedListener pageSelectedListener) {
        listener = pageSelectedListener;
    }

    public void setCurrentItem(int item) {
        int nextItem = Math.max(0, Math.min(item, pages.size() - 1));
        if (nextItem == currentItem && getChildCount() > 0) return;
        cancelSettleAnimation();
        currentItem = nextItem;
        dragOffsetX = 0.0f;
        renderPages();
        if (listener != null) listener.onPageSelected(nextItem);
    }

    private void renderPages() {
        float pageWidth = getWidth();
        if (pageWidth <= 0.0f) return;
        float dragProgress = dragOffsetX / pageWidth;
        for (int pageIndex = 0; pageIndex < getChildCount(); pageIndex++) {
            View page = getChildAt(pageIndex);
            float position = pageIndex - currentItem + dragProgress;
            boolean nearbyPage = Math.abs(position) <= PAGE_TRANSFORM_POSITION_LIMIT;
            page.setVisibility(nearbyPage ? VISIBLE : INVISIBLE);
            page.setTranslationX(position * pageWidth);
            if (transformer != null && nearbyPage) {
                transformer.transformPage(page, position);
            }
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        renderPages();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            horizontalGesture = false;
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getX() - downX;
            float deltaY = event.getY() - downY;
            horizontalGesture = Math.abs(deltaX) > dp(PAGE_DRAG_START_THRESHOLD_DP)
                    && Math.abs(deltaX) > Math.abs(deltaY);
            return horizontalGesture;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelSettleAnimation();
                downX = event.getX();
                downY = event.getY();
                dragOffsetX = 0.0f;
                obtainVelocityTracker().addMovement(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                obtainVelocityTracker().addMovement(event);
                if (horizontalGesture || Math.abs(event.getX() - downX)
                        > dp(PAGE_DRAG_START_THRESHOLD_DP)) {
                    horizontalGesture = true;
                    dragOffsetX = resistedOffset(event.getX() - downX);
                    renderPages();
                }
                return true;
            case MotionEvent.ACTION_UP:
                obtainVelocityTracker().addMovement(event);
                finishGesture();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                releaseVelocityTracker();
                settleToCurrentPage();
                return true;
            default:
                return true;
        }
    }

    private float resistedOffset(float offset) {
        boolean draggingPastFirstPage = currentItem == 0 && offset > 0.0f;
        boolean draggingPastLastPage = currentItem == pages.size() - 1 && offset < 0.0f;
        if (draggingPastFirstPage || draggingPastLastPage) {
            return offset * PAGE_RESISTANCE;
        }
        return offset;
    }

    private void finishGesture() {
        float horizontalVelocity = 0.0f;
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(
                    1000,
                    dp(PAGE_FLING_VELOCITY_DP_PER_SECOND)
            );
            horizontalVelocity = velocityTracker.getXVelocity();
        }
        releaseVelocityTracker();

        int nextItem = currentItem;
        float changeThreshold = dp(PAGE_CHANGE_THRESHOLD_DP);
        float flingThreshold = dp(PAGE_FLING_VELOCITY_DP_PER_SECOND);
        if (dragOffsetX < -changeThreshold || horizontalVelocity < -flingThreshold) {
            nextItem++;
        } else if (dragOffsetX > changeThreshold || horizontalVelocity > flingThreshold) {
            nextItem--;
        }
        nextItem = Math.max(0, Math.min(nextItem, pages.size() - 1));
        settleToItem(nextItem);
    }

    private void settleToCurrentPage() {
        settleToItem(currentItem);
    }

    private void settleToItem(int targetItem) {
        float targetOffset = (currentItem - targetItem) * getWidth();
        float startingOffset = dragOffsetX;
        cancelSettleAnimation();
        if (startingOffset == targetOffset || getWidth() == 0) {
            currentItem = targetItem;
            dragOffsetX = 0.0f;
            renderPages();
            notifyPageSelected(targetItem);
            return;
        }

        settleAnimator = ValueAnimator.ofFloat(startingOffset, targetOffset);
        settleAnimator.setDuration(PAGE_SETTLE_DURATION_MILLIS);
        settleAnimator.setInterpolator(new DecelerateInterpolator());
        settleAnimator.addUpdateListener(animator -> {
            dragOffsetX = (float) animator.getAnimatedValue();
            renderPages();
        });
        settleAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (canceled) return;
                currentItem = targetItem;
                dragOffsetX = 0.0f;
                settleAnimator = null;
                renderPages();
                notifyPageSelected(targetItem);
            }
        });
        settleAnimator.start();
    }

    private void notifyPageSelected(int selectedItem) {
        if (listener != null) {
            listener.onPageSelected(selectedItem);
        }
    }

    private void cancelSettleAnimation() {
        if (settleAnimator != null) {
            settleAnimator.cancel();
            settleAnimator = null;
        }
    }

    private VelocityTracker obtainVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        return velocityTracker;
    }

    private void releaseVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + DP_ROUNDING_OFFSET);
    }
}
