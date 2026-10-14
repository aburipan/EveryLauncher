package jp.aburipan.everylauncher.timer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jp.aburipan.everylauncher.LauncherActivity;
import jp.aburipan.everylauncher.R;
import jp.aburipan.everylauncher.ui.Fonts;
import jp.aburipan.everylauncher.ui.UiColors;
import jp.aburipan.everylauncher.ui.UiDimensions;
import jp.aburipan.everylauncher.ui.UiModes;
import jp.aburipan.everylauncher.ui.UiTextSizes;

public final class TimerController {
    private static final int DEFAULT_TIMER_DURATION_MINUTES = 5;
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MILLIS_PER_TENTH_SECOND = 100L;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3_600;
    private static final int TIMER_UPDATE_INTERVAL_MILLIS = 100;
    private static final int ALARM_TONE_DURATION_MILLIS = 5_000;
    private static final long ALARM_STOP_DELAY_MILLIS = 60_000L;
    private static final int MAX_HOURS_PER_DAY = 23;
    private static final int MAX_MINUTE_OR_SECOND_VALUE = 59;
    private static final int TIMER_END_ROUNDING_MILLIS = 999;
    private static final int TIMER_PICKER_MINIMUM_VALUE = 0;
    private static final int HALF_ALARM_VOLUME_DIVISOR = 2;
    private static final int MAX_ALARM_VOLUME_PERCENT = 100;
    private static final int TIMER_ALERT_COLOR = Color.rgb(255, 138, 128);
    private static final String HOURS_MINUTES_SECONDS_FORMAT = "%d:%02d:%02d";
    private static final String MINUTES_SECONDS_FORMAT = "%d:%02d";
    private static final String TENTH_SECOND_SEPARATOR = ".";
    private static final int[] STANDARD_TIMER_PRESETS_MINUTES = { 1, 5, 10 };
    private static final int[] EXPANDED_TIMER_PRESETS_MINUTES = { 1, 3, 5, 10 };
    private static final int COMPACT_STOPWATCH_BUTTON_WIDTH_DP = 110;
    private static final int COMPACT_TIMER_BUTTON_WIDTH_DP = 120;
    private static final int EXPANDED_BUTTON_WIDTH_DP = 200;

    private static final int IDLE = 0;
    private static final int RUNNING = 1;
    private static final int PAUSED = 2;
    private static final int DONE = 3;

    private final LauncherActivity activity;
    private final List<Runnable> stopwatchUpdaters = new ArrayList<>();
    private final List<Runnable> timerUpdaters = new ArrayList<>();
    private final Runnable alarmStop = this::stopAlarm;
    private long stopwatchAccumulatedMillis;
    private boolean stopwatchRunning;
    private long stopwatchStartedAt;
    private int timerState = IDLE;
    private long timerEndsAt;
    private long timerDurationMillis = DEFAULT_TIMER_DURATION_MINUTES * MILLIS_PER_MINUTE;
    private long timerRemainingMillis = DEFAULT_TIMER_DURATION_MINUTES * MILLIS_PER_MINUTE;
    private boolean fastTickRunning;
    private MediaPlayer player;
    private ToneGenerator toneGenerator;
    private final Runnable fastTick = new Runnable() {
        @Override public void run() {
            timerTick();
            refreshAll();
            if (isRunning()) activity.handler.postDelayed(this, TIMER_UPDATE_INTERVAL_MILLIS);
            else fastTickRunning = false;
        }
    };

    public TimerController(LauncherActivity activity) {
        this.activity = activity;
    }

    public View createToolsPage() {
        TimerPanel sw = stopwatchPanel(false);
        stopwatchUpdaters.add(sw.update);
        LinearLayout swCard = toolCard(activity.getString(R.string.stopwatch), sw.view, view -> activity.openExpanded(true, view));
        TimerPanel tm = timerPanel(false);
        timerUpdaters.add(tm.update);
        LinearLayout tmCard = toolCard(activity.getString(R.string.timer), tm.view, view -> activity.openExpanded(false, view));
        LinearLayout cards = new LinearLayout(activity);
        cards.addView(swCard, activity.lp(0, activity.MATCH, 1.0f));
        LinearLayout.LayoutParams tcp = activity.lp(0, activity.MATCH, 1.0f);
        tcp.leftMargin = activity.dp(UiDimensions.SPACING_20_DP);
        cards.addView(tmCard, tcp);
        TextView alarm = activity.pill(
                activity.getString(R.string.open_alarm), true,
                UiModes.PILL_STANDARD,
                () -> activity.startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS))
        );
        LinearLayout.LayoutParams ap = activity.lp(activity.WRAP, activity.WRAP);
        ap.topMargin = activity.dp(UiDimensions.SPACING_24_DP);
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_24_DP), activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_48_DP));
        page.addView(cards, activity.lp(activity.MATCH, activity.WRAP));
        page.addView(alarm, ap);
        return page;
    }

    private LinearLayout toolCard(String title, View content, View.OnClickListener onClick) {
        LinearLayout card = activity.glassCard();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(activity.label(title, UiTextSizes.SIZE_14_SP, activity.accent, Fonts.SANS_SERIF_MEDIUM));
        card.addView(content, activity.lp(activity.MATCH, 0, 1.0f));
        LinearLayout.LayoutParams hp = activity.lp(activity.WRAP, activity.WRAP);
        hp.topMargin = activity.dp(UiDimensions.SPACING_12_DP);
        card.addView(activity.label(activity.getString(R.string.tap_to_expand), UiTextSizes.SIZE_12_SP, UiColors.HINT_TEXT), hp);
        card.setClickable(true);
        card.setOnClickListener(onClick);
        return card;
    }

    private long swElapsed() {
        return stopwatchAccumulatedMillis + (stopwatchRunning ? SystemClock.elapsedRealtime() - stopwatchStartedAt : 0L);
    }

    private void swToggle() {
        if (stopwatchRunning) {
            stopwatchAccumulatedMillis = swElapsed();
            stopwatchRunning = false;
        } else {
            stopwatchStartedAt = SystemClock.elapsedRealtime();
            stopwatchRunning = true;
        }
        refreshAll();
        ensureTick();
    }

    private void swReset() {
        stopwatchRunning = false;
        stopwatchAccumulatedMillis = 0L;
        refreshAll();
    }

    private TimerPanel stopwatchPanel(boolean expanded) {
        TextView time = activity.label(activity.getString(R.string.stopwatch_initial), expanded ? UiTextSizes.SIZE_120_SP : UiTextSizes.SIZE_56_SP, Color.WHITE, Fonts.SANS_SERIF_THIN);
        time.setGravity(Gravity.CENTER);
        time.setIncludeFontPadding(false);
        TextView startStopButton = activity.pill(
                activity.getString(R.string.start), true,
                expanded ? UiModes.PILL_LARGE : UiModes.PILL_STANDARD,
                this::swToggle
        );
        startStopButton.setMinWidth(activity.dp(expanded
                ? EXPANDED_BUTTON_WIDTH_DP
                : COMPACT_STOPWATCH_BUTTON_WIDTH_DP));
        TextView resetButton = activity.pill(
                activity.getString(R.string.reset), false,
                expanded ? UiModes.PILL_LARGE : UiModes.PILL_STANDARD,
                this::swReset
        );

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setGravity(Gravity.CENTER);
        buttons.addView(startStopButton);
        buttons.addView(resetButton, spaced());
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.addView(time, activity.lp(activity.MATCH, activity.WRAP));
        LinearLayout.LayoutParams buttonsParams = activity.lp(activity.WRAP, activity.WRAP);
        buttonsParams.topMargin = activity.dp(expanded
                ? UiDimensions.SPACING_28_DP
                : UiDimensions.SPACING_16_DP);
        panel.addView(buttons, buttonsParams);

        Runnable update = () -> {
            time.setText(fmtSw(swElapsed(), expanded));
            startStopButton.setText(stopwatchRunning ? activity.getString(R.string.stop) : stopwatchAccumulatedMillis > 0 ? activity.getString(R.string.resume) : activity.getString(R.string.start));
        };
        update.run();
        return new TimerPanel(panel, update);
    }

    private void tmPrimary() {
        switch (timerState) {
            case RUNNING:
                timerRemainingMillis = Math.max(0L, timerEndsAt - SystemClock.elapsedRealtime());
                timerState = PAUSED;
                break;
            case PAUSED:
                timerEndsAt = SystemClock.elapsedRealtime() + timerRemainingMillis;
                timerState = RUNNING;
                break;
            default:
    stopAlarm();
                if (timerDurationMillis > 0) {
                    timerRemainingMillis = timerDurationMillis;
                    timerEndsAt = SystemClock.elapsedRealtime() + timerRemainingMillis;
                    timerState = RUNNING;
                }
                break;
        }
        refreshAll();
        ensureTick();
    }

    private void tmStop() {
    stopAlarm();
        timerState = IDLE;
        timerRemainingMillis = timerDurationMillis;
        refreshAll();
    }

    private void setTimerTotal(long ms, boolean start) {
    stopAlarm();
        timerDurationMillis = ms;
        timerRemainingMillis = ms;
        timerState = IDLE;
        if (start) {
            tmPrimary();
        } else {
            refreshAll();
        }
    }

    private void timerTick() {
        if (timerState == RUNNING) {
            long left = timerEndsAt - SystemClock.elapsedRealtime();
            if (left <= 0) {
                timerRemainingMillis = 0L;
                timerState = DONE;
                startAlarm();
                return;
            }
            timerRemainingMillis = left;
        }
    }

    private TimerPanel timerPanel(boolean expanded) {
        TextView time = activity.label(activity.getString(R.string.timer_initial), expanded ? UiTextSizes.SIZE_120_SP : UiTextSizes.SIZE_56_SP, Color.WHITE, Fonts.SANS_SERIF_THIN);
        time.setGravity(Gravity.CENTER);
        time.setIncludeFontPadding(false);
        TextView startPauseButton = activity.pill(
                activity.getString(R.string.start), true,
                expanded ? UiModes.PILL_LARGE : UiModes.PILL_STANDARD,
                this::tmPrimary
        );
        startPauseButton.setMinWidth(activity.dp(expanded
                ? EXPANDED_BUTTON_WIDTH_DP
                : COMPACT_TIMER_BUTTON_WIDTH_DP));
        TextView stopButton = activity.pill(
                activity.getString(R.string.stop), false,
                expanded ? UiModes.PILL_LARGE : UiModes.PILL_STANDARD,
                this::tmStop
        );

        LinearLayout controls = new LinearLayout(activity);
        controls.setGravity(Gravity.CENTER);
        controls.addView(startPauseButton);
        controls.addView(stopButton, spaced());

        LinearLayout presets = new LinearLayout(activity);
        presets.setGravity(Gravity.CENTER);
        int presetMode = expanded ? UiModes.PILL_STANDARD : UiModes.PILL_COMPACT;
        int[] presetMinutes = expanded
                ? EXPANDED_TIMER_PRESETS_MINUTES
                : STANDARD_TIMER_PRESETS_MINUTES;
        for (int minutes : presetMinutes) {
            presets.addView(activity.pill(activity.getString(R.string.minutes_format, minutes), false, presetMode,
                    () -> setTimerTotal(minutes * MILLIS_PER_MINUTE, true)), spaced());
        }
        presets.addView(activity.pill(expanded ? activity.getString(R.string.set_time) : activity.getString(R.string.settings), false, presetMode,
                this::showTimerPicker), spaced());

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.addView(time, activity.lp(activity.MATCH, activity.WRAP));
        LinearLayout.LayoutParams controlsParams = activity.lp(activity.WRAP, activity.WRAP);
        controlsParams.topMargin = activity.dp(expanded
                ? UiDimensions.SPACING_28_DP
                : UiDimensions.SPACING_16_DP);
        panel.addView(controls, controlsParams);
        LinearLayout.LayoutParams presetsParams = activity.lp(activity.WRAP, activity.WRAP);
        presetsParams.topMargin = activity.dp(expanded
                ? UiDimensions.SPACING_24_DP
                : UiDimensions.SPACING_12_DP);
        panel.addView(presets, presetsParams);

        Runnable update = () -> {
            time.setText(fmtHms(
                    (timerRemainingMillis + TIMER_END_ROUNDING_MILLIS) / MILLIS_PER_SECOND
            ));
            time.setTextColor(timerState == DONE ? TIMER_ALERT_COLOR : Color.WHITE);
            switch (timerState) {
                case RUNNING:
                    startPauseButton.setText(activity.getString(R.string.pause));
                    break;
                case PAUSED:
                    startPauseButton.setText(activity.getString(R.string.resume));
                    break;
                default:
                    startPauseButton.setText(activity.getString(R.string.start));
                    break;
            }
        };
        update.run();
        return new TimerPanel(panel, update);
    }

    private NumberPicker createTimerPicker(int maximumValue, int selectedValue) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(TIMER_PICKER_MINIMUM_VALUE);
        picker.setMaxValue(maximumValue);
        picker.setValue(selectedValue);
        return picker;
    }

    private void showTimerPicker() {
        long totalSeconds = timerDurationMillis / MILLIS_PER_SECOND;
        final NumberPicker hoursPicker = createTimerPicker(
                MAX_HOURS_PER_DAY, (int) (totalSeconds / SECONDS_PER_HOUR)
        );
        final NumberPicker minutesPicker = createTimerPicker(
                MAX_MINUTE_OR_SECOND_VALUE,
                (int) ((totalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR)
        );
        final NumberPicker secondsPicker = createTimerPicker(
                MAX_MINUTE_OR_SECOND_VALUE,
                (int) (totalSeconds % SECONDS_PER_MINUTE)
        );
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setPadding(activity.dp(UiDimensions.SPACING_16_DP), activity.dp(UiDimensions.SPACING_16_DP), activity.dp(UiDimensions.SPACING_16_DP), activity.dp(UiDimensions.SPACING_8_DP));
        row.addView(hoursPicker);
        row.addView(createTimerUnitLabel(activity.getString(R.string.hours)));
        row.addView(minutesPicker);
        row.addView(createTimerUnitLabel(activity.getString(R.string.minutes)));
        row.addView(secondsPicker);
        row.addView(createTimerUnitLabel(activity.getString(R.string.seconds)));
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.timer_duration)).setView(row)
                .setPositiveButton(activity.getString(R.string.start), (dialog, which) -> { long duration = selectedTimerDuration(hoursPicker, minutesPicker, secondsPicker); if (duration > 0) setTimerTotal(duration, true); })
                .setNeutralButton(activity.getString(R.string.set), (dialog, which) -> { long duration = selectedTimerDuration(hoursPicker, minutesPicker, secondsPicker); if (duration > 0) setTimerTotal(duration, false); })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .show();
    }

    private TextView createTimerUnitLabel(String unitText) {
        TextView unitLabel = activity.label(unitText, UiTextSizes.SIZE_16_SP);
        unitLabel.setPadding(activity.dp(UiDimensions.SPACING_6_DP), 0, activity.dp(UiDimensions.SPACING_14_DP), 0);
        return unitLabel;
    }

    private static long selectedTimerDuration(NumberPicker hoursPicker, NumberPicker minutesPicker, NumberPicker secondsPicker) {
        return ((((long) hoursPicker.getValue()) * SECONDS_PER_HOUR)
                + (((long) minutesPicker.getValue()) * SECONDS_PER_MINUTE)
                + secondsPicker.getValue()) * MILLIS_PER_SECOND;
    }

    private void startAlarm() {
    stopAlarm();
        try {
            AudioManager audioManager = (AudioManager) activity.getSystemService(android.content.Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == TIMER_PICKER_MINIMUM_VALUE) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                                / HALF_ALARM_VOLUME_DIVISOR,
                        0);
            }
        } catch (Exception ignored) {
        }
        List<Uri> ringtoneUris = Arrays.asList(
                RingtoneManager.getActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_NOTIFICATION),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        );
        boolean alarmStarted = false;
        for (Uri ringtoneUri : ringtoneUris) {
            if (ringtoneUri != null) {
                MediaPlayer mediaPlayer = null;
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    );
                    mediaPlayer.setDataSource(activity, ringtoneUri);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    player = mediaPlayer;
                    alarmStarted = true;
                    break;
                } catch (Exception releaseError) {
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.release();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        if (!alarmStarted) {
            try {
                ToneGenerator alarmTone = new ToneGenerator(
                        AudioManager.STREAM_ALARM,
                        MAX_ALARM_VOLUME_PERCENT
                );
                alarmTone.startTone(
                        ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                        ALARM_TONE_DURATION_MILLIS
                );
                toneGenerator = alarmTone;
            } catch (Exception ignored) {
            }
        }
        activity.handler.removeCallbacks(alarmStop);
        activity.handler.postDelayed(alarmStop, ALARM_STOP_DELAY_MILLIS);
    }

    public void stopAlarm() {
        activity.handler.removeCallbacks(alarmStop);
        try {
            MediaPlayer mediaPlayer = player;
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
        } catch (Exception ignored) {
        }
        try {
            MediaPlayer currentPlayer = player;
            if (currentPlayer != null) {
                currentPlayer.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
        try {
            ToneGenerator currentToneGenerator = toneGenerator;
            if (currentToneGenerator != null) {
                currentToneGenerator.release();
            }
        } catch (Exception ignored) {
        }
        toneGenerator = null;
    }

    private void refreshAll() {
        for (Runnable updater : stopwatchUpdaters) updater.run();
        for (Runnable updater : timerUpdaters) updater.run();
    }

    private void ensureTick() {
        if (!fastTickRunning) {
            fastTickRunning = true;
            activity.handler.post(fastTick);
        }
    }

    private String fmtHms(long totalSeconds) {
        long hours = totalSeconds / SECONDS_PER_HOUR;
        long minutes = (totalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;
        if (hours > 0) {
            return String.format(
                    Locale.getDefault(), HOURS_MINUTES_SECONDS_FORMAT,
                    hours, minutes, seconds
            );
        }
        return String.format(Locale.getDefault(), MINUTES_SECONDS_FORMAT, minutes, seconds);
    }

    private String fmtSw(long ms, boolean tenths) {
        String base = fmtHms(ms / MILLIS_PER_SECOND);
        if (!tenths) {
            return base;
        }
        return base + TENTH_SECOND_SEPARATOR
                + ((ms / MILLIS_PER_TENTH_SECOND) % 10);
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = activity.lp(activity.WRAP, activity.WRAP);
        params.leftMargin = activity.dp(UiDimensions.SPACING_10_DP);
        return params;
    }
    public TimerPanel createPanel(boolean stopwatch, boolean expanded) {
        return stopwatch ? stopwatchPanel(expanded) : timerPanel(expanded);
    }

    public List<Runnable> updaters(boolean stopwatch) {
        return stopwatch ? stopwatchUpdaters : timerUpdaters;
    }

    private boolean isRunning() {
        return stopwatchRunning || timerState == RUNNING;
    }
}
