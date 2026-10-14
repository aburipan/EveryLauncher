package jp.aburipan.everylauncher.ui.pages;


import android.graphics.Color;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import jp.aburipan.everylauncher.LauncherActivity;
import jp.aburipan.everylauncher.R;
import jp.aburipan.everylauncher.calendar.CalendarEvent;
import jp.aburipan.everylauncher.calendar.CalendarRepository;
import jp.aburipan.everylauncher.ui.Fonts;
import jp.aburipan.everylauncher.ui.UiDimensions;
import jp.aburipan.everylauncher.ui.UiTextSizes;

import java.util.List;

public final class CalendarScreen {
    private final LauncherActivity activity;
    private LinearLayout eventList;
    private ScrollView scrollView;

    public CalendarScreen(LauncherActivity activity) {
        this.activity = activity;
    }

    public View createPage() {
        eventList = new LinearLayout(activity);
        eventList.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(eventList);
        scrollView = scroll;
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_32_DP), activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_48_DP));
        TextView header = activity.label(activity.getString(R.string.calendar_next_days_title), UiTextSizes.SIZE_28_SP, Color.WHITE, Fonts.SANS_SERIF_LIGHT);
        LinearLayout.LayoutParams hp = activity.lp(activity.WRAP, activity.WRAP);
        hp.bottomMargin = activity.dp(UiDimensions.SPACING_16_DP);
        page.addView(header, hp);
        page.addView(scroll, activity.lp(activity.MATCH, 0, 1.0f));
        return page;
    }

    private View eventRow(String whenText, String title) {
        TextView t = activity.label(whenText, UiTextSizes.SIZE_15_SP, activity.accent, Fonts.SANS_SERIF_MEDIUM);
        t.setMinWidth(activity.dp(UiDimensions.EVENT_TIME_MIN_WIDTH_DP));
        TextView n = activity.label(title, UiTextSizes.SIZE_20_SP);
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(activity.rounded(
                activity.glass,
                UiDimensions.CALENDAR_EVENT_CORNER_RADIUS_DP,
                activity.glassStroke
        ));
        row.setPadding(activity.dp(UiDimensions.SPACING_24_DP), activity.dp(UiDimensions.SPACING_16_DP), activity.dp(UiDimensions.SPACING_24_DP), activity.dp(UiDimensions.SPACING_16_DP));
        row.addView(t);
        row.addView(n);
        LinearLayout.LayoutParams p = activity.lp(activity.MATCH, activity.WRAP);
        p.bottomMargin = activity.dp(UiDimensions.SPACING_10_DP);
        row.setLayoutParams(p);
        return row;
    }

    public void refresh() {
        int previousScrollY = scrollView == null ? 0 : scrollView.getScrollY();
        if (eventList == null || scrollView == null) return;

        eventList.removeAllViews();
        if (activity.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            eventList.addView(activity.label(activity.getString(R.string.calendar_permission_required), UiTextSizes.SIZE_20_SP, activity.softWhite, Fonts.SANS_SERIF_LIGHT));
            activity.setNextEventText(activity.getString(R.string.permission_required));
            return;
        }

        List<CalendarEvent> events =
                CalendarRepository.load(activity, System.currentTimeMillis());
        String nextEvent = null;
        for (CalendarEvent event : events) {
            eventList.addView(eventRow(event.dateLabel, event.title));
            if (nextEvent == null) nextEvent = activity.getString(R.string.next_event_format, event.dateLabel, event.title);
        }
        if (events.isEmpty()) {
            eventList.addView(activity.label(activity.getString(R.string.calendar_empty), UiTextSizes.SIZE_20_SP,
                    activity.softWhite, Fonts.SANS_SERIF_LIGHT));
        }
        activity.setNextEventText(nextEvent == null ? activity.getString(R.string.calendar_no_events) : nextEvent);
        scrollView.post(() -> scrollView.scrollTo(0, previousScrollY));
    }
}
