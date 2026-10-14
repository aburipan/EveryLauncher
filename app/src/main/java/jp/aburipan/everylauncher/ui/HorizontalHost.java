package jp.aburipan.everylauncher.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.FrameLayout;

public final class HorizontalHost extends FrameLayout {
    public HorizontalHost(Context context) { super(context); }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        ViewParent parent = getParent();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        return super.dispatchTouchEvent(event);
    }
}
