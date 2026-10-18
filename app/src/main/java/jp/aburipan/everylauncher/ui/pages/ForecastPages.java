package jp.aburipan.everylauncher.ui.pages;


import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import jp.aburipan.everylauncher.AppConstants;
import jp.aburipan.everylauncher.LauncherActivity;
import jp.aburipan.everylauncher.R;
import jp.aburipan.everylauncher.ui.Fonts;
import jp.aburipan.everylauncher.ui.HorizontalHost;
import jp.aburipan.everylauncher.ui.UiColors;
import jp.aburipan.everylauncher.ui.UiDimensions;
import jp.aburipan.everylauncher.ui.UiTextSizes;
import jp.aburipan.everylauncher.weather.DayForecast;
import jp.aburipan.everylauncher.weather.Forecast;
import jp.aburipan.everylauncher.weather.HourForecast;

import java.util.Collections;
import java.util.List;

public final class ForecastPages {
    private static final int HOUR_PAGE_TOP_PADDING_DP = UiDimensions.SPACING_32_DP;
    private static final int HOUR_PAGE_BOTTOM_SPACE_DP = UiDimensions.SPACING_16_DP;
    private static final int HOUR_TITLE_HEIGHT_DP = 38;
    private static final int HOUR_TILE_HEIGHT_DP = 204;
    private static final float MINIMUM_HOUR_TILE_SCALE = 0.7f;
    private static final float MAXIMUM_HOUR_TILE_SCALE = 1.0f;
    private static final int GRID_COLUMN_COUNT = 6;
    private static final int GRID_GAP_COUNT = GRID_COLUMN_COUNT - 1;
    private static final int SCREEN_HEIGHT_PADDING_DIVISOR = 3;
    private static final int TILE_SCALE_SCREEN_HEIGHT_NUMERATOR = 2;
    private static final int TILE_SCALE_SCREEN_HEIGHT_DENOMINATOR = 3;
    private static final int HOUR_TILE_VERTICAL_GAP_DP = UiDimensions.SPACING_8_DP;

    private final LauncherActivity activity;
    private List<HourForecast> hourlyForecasts = Collections.emptyList();
    private List<DayForecast> dailyForecasts = Collections.emptyList();
    private LinearLayout hourForecastRow;
    private HorizontalScrollView hourForecastScrollView;

    public ForecastPages(LauncherActivity activity) {
        this.activity = activity;
    }

    public void setForecast(Forecast forecast) {
        hourlyForecasts = forecast.hourly;
        dailyForecasts = forecast.daily;
    buildHourly();
    }

    public View hourlyPage() {
        hourForecastRow = new LinearLayout(activity);
        hourForecastRow.setOrientation(LinearLayout.HORIZONTAL);
        hourForecastScrollView = new HorizontalScrollView(activity);
        hourForecastScrollView.setHorizontalScrollBarEnabled(false);
        hourForecastScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hourForecastScrollView.addView(hourForecastRow);

        HorizontalHost host = new HorizontalHost(activity);
        host.addView(hourForecastScrollView, new FrameLayout.LayoutParams(activity.MATCH, activity.WRAP));
        int sidePadding = activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP);
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        int screenHeight = Math.min(activity.getResources().getDisplayMetrics().widthPixels,
                activity.getResources().getDisplayMetrics().heightPixels);
        page.setPadding(
                0,
                activity.dp(UiDimensions.SPACING_32_DP),
                0,
                screenHeight / SCREEN_HEIGHT_PADDING_DIVISOR
        );
        LinearLayout.LayoutParams titleParams = activity.lp(activity.WRAP, activity.WRAP);
        titleParams.leftMargin = activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP);
        page.addView(activity.label(activity.getString(R.string.forecast_24_hours), UiTextSizes.SIZE_28_SP, Color.WHITE, Fonts.SANS_SERIF_LIGHT), titleParams);
        page.addView(new View(activity), activity.lp(activity.MATCH, 0, 1.0f));
        LinearLayout.LayoutParams hostParams = activity.lp(activity.MATCH, activity.WRAP);
        hostParams.setMargins(sidePadding, 0, sidePadding, 0);
        page.addView(host, hostParams);
    buildHourly();
        return page;
    }

    public void buildHourly() {
        int scrollX = hourForecastScrollView.getScrollX();
        hourForecastRow.removeAllViews();
        if (hourlyForecasts.isEmpty()) {
            hourForecastRow.addView(activity.label(activity.getString(R.string.forecast_loading), UiTextSizes.SIZE_20_SP, activity.softWhite));
            return;
        }

        int screenWidth = Math.max(activity.getResources().getDisplayMetrics().widthPixels,
                activity.getResources().getDisplayMetrics().heightPixels);
        int gap = activity.dp(UiDimensions.SPACING_8_DP);
        int availableWidth = screenWidth - 2 * activity.dp(UiDimensions.PAGE_SIDE_PADDING_DP);
        int tileWidth = (availableWidth - GRID_GAP_COUNT * gap) / GRID_COLUMN_COUNT;
        for (int index = 0; index < hourlyForecasts.size(); index++) {
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(tileWidth, activity.WRAP);
            tileParams.setMargins(0, 0, gap, 0);
            hourForecastRow.addView(hourTile(hourlyForecasts.get(index), index == 0), tileParams);
        }
        hourForecastScrollView.post(() -> hourForecastScrollView.scrollTo(scrollX, 0));
    }

    private float hourScale() {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float screenHeightDp = Math.min(metrics.widthPixels, metrics.heightPixels) / metrics.density;
        float availableHeight = screenHeightDp * TILE_SCALE_SCREEN_HEIGHT_NUMERATOR
                / TILE_SCALE_SCREEN_HEIGHT_DENOMINATOR
                - HOUR_PAGE_TOP_PADDING_DP
                - HOUR_TITLE_HEIGHT_DP
                - HOUR_PAGE_BOTTOM_SPACE_DP;
        return Math.max(
                MINIMUM_HOUR_TILE_SCALE,
                Math.min(MAXIMUM_HOUR_TILE_SCALE, availableHeight / HOUR_TILE_HEIGHT_DP)
        );
    }

    private View hourTile(HourForecast forecast, boolean isNow) {
        float scale = hourScale();
        String icon = activity.weatherLabel(forecast.weatherCode, forecast.isDay).first;
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        int verticalPadding = activity.dp((int) (UiDimensions.SPACING_12_DP * scale));
        tile.setPadding(activity.dp(UiDimensions.SPACING_4_DP), verticalPadding, activity.dp(UiDimensions.SPACING_4_DP), verticalPadding);
        tile.setBackground(isNow
                ? activity.rounded(UiColors.SELECTED_FORECAST_BACKGROUND,
                        UiDimensions.FORECAST_CARD_CORNER_RADIUS_DP, activity.accent)
                : activity.rounded(activity.glass,
                        UiDimensions.FORECAST_CARD_CORNER_RADIUS_DP, activity.glassStroke));
        tile.addView(activity.label(forecast.dateLabel.isEmpty()
                        ? AppConstants.NON_BREAKING_SPACE_PLACEHOLDER
                        : forecast.dateLabel,
                UiTextSizes.SIZE_12_SP * scale, activity.accent, Fonts.SANS_SERIF_MEDIUM));
        String hourLabel = isNow ? activity.getString(R.string.now) : activity.getString(R.string.forecast_hour_format, forecast.hour);
        tile.addView(activity.label(hourLabel, UiTextSizes.SIZE_19_SP * scale,
                isNow ? activity.accent : Color.WHITE, Fonts.SANS_SERIF_MEDIUM));
        LinearLayout.LayoutParams iconParams = activity.lp(activity.WRAP, activity.WRAP);
        iconParams.topMargin = activity.dp((int) (HOUR_TILE_VERTICAL_GAP_DP * scale));
        tile.addView(activity.label(icon, UiTextSizes.SIZE_50_SP * scale), iconParams);
        LinearLayout.LayoutParams temperatureParams = activity.lp(activity.WRAP, activity.WRAP);
        temperatureParams.topMargin = activity.dp((int) (HOUR_TILE_VERTICAL_GAP_DP * scale));
        tile.addView(activity.label(activity.getString(R.string.forecast_temperature_format, forecast.temperature), UiTextSizes.SIZE_32_SP * scale), temperatureParams);
        tile.addView(activity.label(activity.getString(R.string.precipitation_format, forecast.precipitationChance), UiTextSizes.SIZE_14_SP * scale, activity.softWhite));
        return tile;
    }

    public void openForecast(View origin) {
        activity.dismissOverlayNow();
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(activity.dp(UiDimensions.SPACING_32_DP), activity.dp(UiDimensions.SPACING_16_DP), activity.dp(UiDimensions.SPACING_32_DP), activity.dp(UiDimensions.SPACING_16_DP));
        col.addView(activity.label(activity.getString(R.string.forecast_7_days), UiTextSizes.SIZE_24_SP, activity.accent, Fonts.SANS_SERIF_MEDIUM));
        List<DayForecast> forecasts = dailyForecasts;
        if (forecasts.isEmpty()) {
            col.addView(activity.label(activity.getString(R.string.forecast_loading), UiTextSizes.SIZE_20_SP, activity.softWhite));
        } else {
            LinearLayout row = new LinearLayout(activity);
            for (int forecastIndex = 0; forecastIndex < forecasts.size(); forecastIndex++) {
                LinearLayout.LayoutParams tileParams = activity.lp(0, activity.WRAP, 1.0f);
                tileParams.setMargins(activity.dp(UiDimensions.SPACING_4_DP), 0, activity.dp(UiDimensions.SPACING_4_DP), 0);
                DayForecast dayForecast = forecasts.get(forecastIndex);
                row.addView(dayTile(dayForecast, forecastIndex == 0), tileParams);
            }
            LinearLayout.LayoutParams rp = activity.lp(activity.MATCH, activity.WRAP);
            rp.topMargin = activity.dp(UiDimensions.SPACING_24_DP);
            col.addView(row, rp);
        }
        activity.showOverlay(col, origin);
    }

    private View dayTile(DayForecast forecast, boolean isToday) {
        Pair<String, String> pairWeatherLabel = activity.weatherLabel(forecast.weatherCode, true);
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(activity.dp(UiDimensions.SPACING_6_DP), activity.dp(UiDimensions.SPACING_20_DP), activity.dp(UiDimensions.SPACING_6_DP), activity.dp(UiDimensions.SPACING_20_DP));
        col.setBackground(isToday
                ? activity.rounded(UiColors.SELECTED_FORECAST_BACKGROUND,
                        UiDimensions.FORECAST_CARD_CORNER_RADIUS_DP, activity.accent)
                : activity.rounded(activity.glass,
                        UiDimensions.FORECAST_CARD_CORNER_RADIUS_DP, activity.glassStroke));
        col.addView(activity.label(forecast.dateLabel, UiTextSizes.SIZE_17_SP,
                isToday ? activity.accent : Color.WHITE, Fonts.SANS_SERIF_MEDIUM));
        LinearLayout.LayoutParams ip = activity.lp(activity.WRAP, activity.WRAP);
        ip.topMargin = activity.dp(UiDimensions.SPACING_10_DP);
        col.addView(activity.label(pairWeatherLabel.first, UiTextSizes.SIZE_38_SP), ip);
        col.addView(activity.label(pairWeatherLabel.second, UiTextSizes.SIZE_13_SP, activity.softWhite));
        LinearLayout.LayoutParams tp = activity.lp(activity.WRAP, activity.WRAP);
        tp.topMargin = activity.dp(UiDimensions.SPACING_10_DP);
        col.addView(activity.label(activity.getString(R.string.forecast_temperature_format, forecast.highTemperature), UiTextSizes.SIZE_28_SP), tp);
        col.addView(activity.label(activity.getString(R.string.forecast_temperature_format, forecast.lowTemperature), UiTextSizes.SIZE_20_SP, activity.softWhite));
        LinearLayout.LayoutParams pp = activity.lp(activity.WRAP, activity.WRAP);
        pp.topMargin = activity.dp(UiDimensions.SPACING_8_DP);
        col.addView(activity.label(activity.getString(R.string.precipitation_format, forecast.precipitationChance), UiTextSizes.SIZE_13_SP, activity.softWhite), pp);
        return col;
    }
}
