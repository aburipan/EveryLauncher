package jp.aburipan.everylauncher.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import jp.aburipan.everylauncher.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class CalendarRepository {
    private static final long PAST_EVENT_LOOKBACK_MILLIS = 86_400_000L;
    private static final long EVENT_LOOKAHEAD_DAYS = 7L;
    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final int EVENT_LIMIT = 20;
    private static final int EVENT_TITLE_COLUMN = 0;
    private static final int EVENT_BEGIN_COLUMN = 1;
    private static final int EVENT_ALL_DAY_COLUMN = 2;
    private static final int EVENT_END_COLUMN = 3;
    private static final int ALL_DAY_VALUE = 1;
    private static final String UTC_TIME_ZONE = "UTC";
    private static final String TITLE_COLUMN = "title";
    private static final String BEGIN_COLUMN = "begin";
    private static final String ALL_DAY_COLUMN = "allDay";
    private static final String END_COLUMN = "end";
    private static final String EVENT_SORT_ORDER = "begin ASC";

    private CalendarRepository() { }

    public static List<CalendarEvent> load(Context context, long now) {
        List<CalendarEvent> events = new ArrayList<>();
        Uri.Builder range = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(range, now - PAST_EVENT_LOOKBACK_MILLIS);
        ContentUris.appendId(range, now + EVENT_LOOKAHEAD_DAYS * MILLIS_PER_DAY);

        SimpleDateFormat timedFormat = new SimpleDateFormat(context.getString(R.string.calendar_timed_date_format), Locale.getDefault());
        SimpleDateFormat allDayFormat = new SimpleDateFormat(context.getString(R.string.calendar_all_day_date_format), Locale.getDefault());
        allDayFormat.setTimeZone(TimeZone.getTimeZone(UTC_TIME_ZONE));

        try (Cursor cursor = context.getContentResolver().query(
                range.build(), new String[] {
                        TITLE_COLUMN, BEGIN_COLUMN, ALL_DAY_COLUMN, END_COLUMN
                },
                null, null, EVENT_SORT_ORDER)) {
            if (cursor != null) {
                while (cursor.moveToNext() && events.size() < EVENT_LIMIT) {
                    String title = cursor.getString(EVENT_TITLE_COLUMN);
                    if (title == null) title = context.getString(R.string.calendar_no_title);
                    long begin = cursor.getLong(EVENT_BEGIN_COLUMN);
                    boolean allDay = cursor.getInt(EVENT_ALL_DAY_COLUMN) == ALL_DAY_VALUE;
                    long rawEnd = cursor.getLong(EVENT_END_COLUMN);
                    long end = allDay ? allDayToLocal(rawEnd) : rawEnd;
                    if (end <= now) continue;

                    String dateLabel = allDay
                            ? context.getString(R.string.calendar_all_day_format, allDayFormat.format(new Date(begin)))
                            : timedFormat.format(new Date(begin));
                    events.add(new CalendarEvent(dateLabel, title));
                }
            }
        } catch (Exception ignored) {
        }
        return events;
    }

    private static long allDayToLocal(long utcMillis) {
        Calendar utcDate = Calendar.getInstance(TimeZone.getTimeZone(UTC_TIME_ZONE));
        utcDate.setTimeInMillis(utcMillis);
        Calendar localDate = Calendar.getInstance();
        localDate.clear();
        localDate.set(utcDate.get(Calendar.YEAR), utcDate.get(Calendar.MONTH),
                utcDate.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return localDate.getTimeInMillis();
    }

}
