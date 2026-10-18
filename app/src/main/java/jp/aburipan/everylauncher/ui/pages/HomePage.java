package jp.aburipan.everylauncher.ui.pages;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import jp.aburipan.everylauncher.AppConstants;
import jp.aburipan.everylauncher.LauncherActivity;
import jp.aburipan.everylauncher.R;
import jp.aburipan.everylauncher.ui.Fonts;
import jp.aburipan.everylauncher.ui.UiColors;
import jp.aburipan.everylauncher.ui.UiDimensions;
import jp.aburipan.everylauncher.ui.UiModes;
import jp.aburipan.everylauncher.ui.UiTextSizes;

public final class HomePage {
    private static final float CLOCK_COLUMN_WEIGHT = 1.2f;
    private static final float INFORMATION_COLUMN_WEIGHT = 1.0f;
    private final LauncherActivity activity;
    private TextView greetingText;
    private TextView weatherIconText;
    private TextView weatherTemperatureText;
    private TextView weatherDescriptionText;
    private TextView weatherRangeText;
    private TextView nextEventText;

    public HomePage(LauncherActivity activity) {
        this.activity = activity;
    }

    public View create() {
        greetingText = activity.label(AppConstants.EMPTY_STRING, UiTextSizes.SIZE_20_SP, activity.softWhite);

        TextClock clock = new TextClock(activity);
        clock.setFormat12Hour(activity.getString(R.string.clock_format));
        clock.setFormat24Hour(activity.getString(R.string.clock_format));
        clock.setTextSize(UiTextSizes.SIZE_120_SP);
        clock.setTextColor(Color.WHITE);
        clock.setTypeface(Typeface.create(Fonts.SANS_SERIF_THIN, Typeface.NORMAL));
        clock.setIncludeFontPadding(false);

        TextClock date = new TextClock(activity);
        date.setFormat12Hour(activity.getString(R.string.home_date_format));
        date.setFormat24Hour(activity.getString(R.string.home_date_format));
        date.setTextSize(UiTextSizes.SIZE_26_SP);
        date.setTextColor(activity.softWhite);
        date.setTypeface(Typeface.create(Fonts.SANS_SERIF_LIGHT, Typeface.NORMAL));
        activity.shade(greetingText);
        activity.shade(clock);
        activity.shade(date);

        LinearLayout clockColumn = new LinearLayout(activity);
        clockColumn.setOrientation(LinearLayout.VERTICAL);
        clockColumn.setGravity(Gravity.CENTER_VERTICAL);
        clockColumn.setTranslationY(-activity.dp(UiDimensions.SPACING_48_DP));
        LinearLayout clockBox = new LinearLayout(activity);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.addView(greetingText);
        clockBox.addView(clock);
        clockBox.addView(date);
        clockBox.setClickable(true);
        clockBox.setOnClickListener(activity::openClock);
        clockColumn.addView(clockBox);
        LinearLayout.LayoutParams appsParams = activity.lp(activity.WRAP, activity.WRAP);
        appsParams.topMargin = activity.dp(UiDimensions.SPACING_28_DP);
        clockColumn.addView(activity.pill(
                activity.getString(R.string.apps), false,
                UiModes.PILL_STANDARD, activity::showApps
        ), appsParams);

        weatherIconText = activity.label(AppConstants.EMPTY_STRING, UiTextSizes.SIZE_56_SP);
        weatherTemperatureText = activity.label(activity.getString(R.string.unknown_temperature), UiTextSizes.SIZE_64_SP, Color.WHITE, Fonts.SANS_SERIF_THIN);
        weatherDescriptionText = activity.label(activity.getString(R.string.weather_loading), UiTextSizes.SIZE_20_SP);
        weatherRangeText = activity.label(AppConstants.EMPTY_STRING, UiTextSizes.SIZE_16_SP, activity.softWhite);
        LinearLayout weatherTopRow = new LinearLayout(activity);
        weatherTopRow.setGravity(Gravity.CENTER_VERTICAL);
        weatherTopRow.addView(weatherIconText);
        LinearLayout.LayoutParams temperatureParams = activity.lp(activity.WRAP, activity.WRAP);
        temperatureParams.leftMargin = activity.dp(UiDimensions.SPACING_16_DP);
        weatherTopRow.addView(weatherTemperatureText, temperatureParams);

        LinearLayout weatherCard = activity.glassCard();
        weatherCard.addView(weatherTopRow);
        weatherCard.addView(weatherDescriptionText);
        weatherCard.addView(weatherRangeText);
        LinearLayout.LayoutParams forecastHintParams = activity.lp(activity.WRAP, activity.WRAP);
        forecastHintParams.topMargin = activity.dp(UiDimensions.SPACING_8_DP);
        weatherCard.addView(activity.label(activity.getString(R.string.forecast_hint), UiTextSizes.SIZE_12_SP,
                UiColors.HINT_TEXT), forecastHintParams);
        weatherCard.setClickable(true);
        weatherCard.setOnClickListener(activity::openForecast);

        nextEventText = activity.label(AppConstants.ELLIPSIS_PLACEHOLDER, UiTextSizes.SIZE_18_SP);
        LinearLayout nextCard = activity.glassCard();
        nextCard.addView(activity.label(activity.getString(R.string.next_event), UiTextSizes.SIZE_14_SP, activity.accent, Fonts.SANS_SERIF_MEDIUM));
        nextCard.addView(nextEventText);
        LinearLayout.LayoutParams eventHintParams = activity.lp(activity.WRAP, activity.WRAP);
        eventHintParams.topMargin = activity.dp(UiDimensions.SPACING_8_DP);
        nextCard.addView(activity.label(activity.getString(R.string.calendar_hint), UiTextSizes.SIZE_12_SP,
                UiColors.HINT_TEXT), eventHintParams);
        nextCard.setClickable(true);
        nextCard.setOnClickListener(view -> activity.openCalendarPage());

        LinearLayout informationColumn = new LinearLayout(activity);
        informationColumn.setOrientation(LinearLayout.VERTICAL);
        informationColumn.setGravity(Gravity.CENTER_VERTICAL);
        informationColumn.setTranslationY(activity.dp(UiDimensions.SPACING_12_DP));
        informationColumn.addView(weatherCard, activity.lp(activity.MATCH, activity.WRAP));
        LinearLayout.LayoutParams nextCardParams = activity.lp(activity.MATCH, activity.WRAP);
        nextCardParams.topMargin = activity.dp(UiDimensions.SPACING_16_DP);
        informationColumn.addView(nextCard, nextCardParams);

        LinearLayout page = new LinearLayout(activity);
        page.setPadding(activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_24_DP), activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP), activity.dp(UiDimensions.SPACING_48_DP));
        page.addView(clockColumn, activity.lp(0, activity.MATCH, CLOCK_COLUMN_WEIGHT));
        LinearLayout.LayoutParams rightParams = activity.lp(
                0, activity.MATCH, INFORMATION_COLUMN_WEIGHT
        );
        rightParams.leftMargin = activity.dp(UiDimensions.SPACING_32_DP);
        page.addView(informationColumn, rightParams);
        return page;
    }

    public void setGreeting(String greeting) {
        greetingText.setText(greeting);
    }

    public void showWeatherMessage(String message) {
        weatherIconText.setText(R.string.location_icon);
        weatherTemperatureText.setText(R.string.unknown_temperature);
        weatherDescriptionText.setText(message);
        weatherRangeText.setText(AppConstants.EMPTY_STRING);
    }

    public void showWeather(String icon, String temperature, String description, String range) {
        weatherIconText.setText(icon);
        weatherTemperatureText.setText(temperature);
        weatherDescriptionText.setText(description);
        weatherRangeText.setText(range);
    }

    public void setNextEvent(String text) {
        nextEventText.setText(text);
    }

}
