package jp.aburipan.everylauncher.timer;

import android.view.View;

public final class TimerPanel {
    public final Runnable update;
    public final View view;

    public TimerPanel(View view, Runnable update) {
        this.view = view;
        this.update = update;
    }
}
