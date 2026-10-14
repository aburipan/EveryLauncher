package jp.aburipan.everylauncher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.util.Pair;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import jp.aburipan.everylauncher.timer.TimerController;
import jp.aburipan.everylauncher.timer.TimerPanel;
import jp.aburipan.everylauncher.ui.AppPickerDialog;
import jp.aburipan.everylauncher.ui.Fonts;
import jp.aburipan.everylauncher.ui.UiColors;
import jp.aburipan.everylauncher.ui.UiDimensions;
import jp.aburipan.everylauncher.ui.UiModes;
import jp.aburipan.everylauncher.ui.UiValues;
import jp.aburipan.everylauncher.ui.UiTextSizes;
import jp.aburipan.everylauncher.ui.NativePager;
import jp.aburipan.everylauncher.ui.pages.CalendarScreen;
import jp.aburipan.everylauncher.ui.pages.ForecastPages;
import jp.aburipan.everylauncher.ui.pages.HomePage;
import jp.aburipan.everylauncher.weather.Forecast;
import jp.aburipan.everylauncher.weather.WeatherCodes;
import jp.aburipan.everylauncher.weather.WeatherLoader;
import jp.aburipan.everylauncher.weather.WeatherView;

@SuppressWarnings({"NullableProblems", "deprecation"})
public final class LauncherActivity extends Activity {
    private static final int WEATHER_REFRESH_INTERVAL_TICKS = 30;
    private static final long UI_REFRESH_INTERVAL_MILLIS = 30_000L;
    private static final int HOME_PAGE_INDEX = 1;
    private static final int CALENDAR_PAGE_INDEX = 2;
    private static final int PAGE_COUNT = 4;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int WINDOW_COORDINATE_COUNT = 2;
    private static final int DOT_SIZE_DP = UiDimensions.SPACING_8_DP;
    private static final int DOT_MARGIN_DP = UiDimensions.SPACING_4_DP;
    private static final int SELECTED_DOT_WIDTH_DP = 22;
    private static final int PILL_STANDARD_TEXT_SIZE_SP = 16;
    private static final int PILL_LARGE_TEXT_SIZE_SP = 22;
    private static final int PILL_COMPACT_TEXT_SIZE_SP = 13;
    private static final int PRIMARY_PILL_HORIZONTAL_PADDING_DP = 36;
    private static final int COMPACT_PILL_HORIZONTAL_PADDING_DP = UiDimensions.SPACING_12_DP;
    private static final int STANDARD_PILL_HORIZONTAL_PADDING_DP = UiDimensions.SPACING_24_DP;
    private static final int PRIMARY_PILL_VERTICAL_PADDING_DP = 18;
    private static final int COMPACT_PILL_VERTICAL_PADDING_DP = UiDimensions.SPACING_8_DP;
    private static final int STANDARD_PILL_VERTICAL_PADDING_DP = UiDimensions.SPACING_12_DP;
    private static final int PRIMARY_PILL_CORNER_RADIUS_DP = 40;
    private static final int STANDARD_PILL_CORNER_RADIUS_DP = 28;
    private static final int CARD_CORNER_RADIUS_DP = 28;
    private static final int SELECTED_DOT_CORNER_RADIUS_DP = UiDimensions.SPACING_4_DP;
    private static final int UNSELECTED_DOT_WIDTH_DP = UiDimensions.SPACING_8_DP;
    private static final int MORNING_THEME_START_HOUR = 5;
    private static final int MORNING_THEME_END_HOUR = 10;
    private static final int DAY_THEME_START_HOUR = 10;
    private static final int DAY_THEME_END_HOUR = 17;
    private static final int EVENING_THEME_START_HOUR = 17;
    private static final int EVENING_THEME_END_HOUR = 19;
    private static final int MORNING_GREETING_START_HOUR = 5;
    private static final int MORNING_GREETING_END_HOUR = 11;
    private static final int DAY_GREETING_START_HOUR = 11;
    private static final int DAY_GREETING_END_HOUR = 18;
    private static final long OVERLAY_OPEN_DURATION_MILLIS = 260L;
    private static final long OVERLAY_CLOSE_DURATION_MILLIS = 200L;
    private static final long PAGE_FADE_DURATION_MILLIS = 220L;
    private static final float OVERLAY_ORIGIN_SCALE = 0.3f;
    private static final float OVERLAY_DEFAULT_SCALE = 0.94f;
    private static final float PAGE_TRANSFORM_SCALE_FACTOR = 0.1f;
    private static final float PAGE_TRANSFORM_ALPHA_FACTOR = 0.7f;
    private static final float OVERLAY_OPEN_DECELERATION = 1.6f;
    private static final float OVERLAY_CLOSE_ACCELERATION = 1.4f;

    private LinearLayout dotRow;
    private View overlay;
    private List<Runnable> overlayUpdaterList;
    private Runnable overlayUpdater;
    NativePager pager;
    private FrameLayout root;
    private int tick;
    private WeatherView weatherView;
    HomePage homeScreen;
    private ForecastPages forecastPages;
    private CalendarScreen calendarScreen;
    private WeatherLoader weatherLoader;
    private TimerController timerController;
    public final int accent = UiColors.ACCENT;
    public final int glass = UiColors.GLASS_BACKGROUND;
    public final int glassStroke = UiColors.GLASS_STROKE;
    public final int softWhite = UiColors.SOFT_WHITE;
    public final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private final List<View> dots = new ArrayList<>();
    public final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            applyTheme();
            tick++;
            loadCalendar();
            if (tick % WEATHER_REFRESH_INTERVAL_TICKS == 0) loadWeather();
            handler.postDelayed(ticker, UI_REFRESH_INTERVAL_MILLIS);
        }
    };
    private float overlayShrink = OVERLAY_DEFAULT_SCALE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        root = new FrameLayout(this);
        weatherView = new WeatherView(this);
        root.addView(weatherView, new FrameLayout.LayoutParams(MATCH, MATCH));

        homeScreen = new HomePage(this);
        forecastPages = new ForecastPages(this);
        calendarScreen = new CalendarScreen(this);
        weatherLoader = new WeatherLoader(this, this::displayWeather, this::weatherMessage);
        timerController = new TimerController(this);
        pager = new NativePager(this);
        pager.setPages(Arrays.asList(hourlyPage(), homePage(), calendarPage(), toolsPage()));
        pager.setCurrentItem(HOME_PAGE_INDEX);
        pager.setPageTransformer((page, position) -> {
            float distance = Math.abs(position);
            float scale = 1.0f - distance * PAGE_TRANSFORM_SCALE_FACTOR;
            page.setAlpha(1.0f - distance * PAGE_TRANSFORM_ALPHA_FACTOR);
            page.setScaleX(scale);
            page.setScaleY(scale);
        });
        root.addView(pager, new FrameLayout.LayoutParams(MATCH, MATCH));

        dotRow = new LinearLayout(this);
        dotRow.setGravity(Gravity.CENTER);
        for (int pageIndex = 0; pageIndex < PAGE_COUNT; pageIndex++) {
            View dot = new View(this);
            dots.add(dot);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    dp(DOT_SIZE_DP), dp(DOT_SIZE_DP)
            );
            dotParams.setMargins(dp(DOT_MARGIN_DP), 0, dp(DOT_MARGIN_DP), 0);
            dotRow.addView(dot, dotParams);
        }
        FrameLayout.LayoutParams dotRowParams = new FrameLayout.LayoutParams(
                WRAP, WRAP, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dotRowParams.bottomMargin = dp(UiDimensions.SPACING_14_DP);
        root.addView(dotRow, dotRowParams);
        pager.setOnPageSelectedListener(this::updateDots);
        updateDots(HOME_PAGE_INDEX);

        setContentView(root);
        applyTheme();
        hideSystemUi();
        requestPermissions(
                new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_CALENDAR
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, UI_REFRESH_INTERVAL_MILLIS);
        loadWeather();
        loadCalendar();
        weatherView.refreshImages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        timerController.stopAlarm();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        loadWeather();
        loadCalendar();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (overlay != null) {
            closeExpanded();
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    public int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public LinearLayout.LayoutParams lp(int width, int height) {
        return lp(width, height, 0f);
    }

    public LinearLayout.LayoutParams lp(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    public GradientDrawable rounded(int color, int radiusDp) { return rounded(color, radiusDp, null); }

    public GradientDrawable rounded(int color, int radiusDp, Integer stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        if (stroke != null) {
            background.setStroke(dp(UiDimensions.BORDER_THIN_DP), stroke);
        }
        return background;
    }

    public TextView label(String text, float sp) { return label(text, sp, Color.WHITE, Fonts.SANS_SERIF_LIGHT); }
    public TextView label(String text, float sp, int color) { return label(text, sp, color, Fonts.SANS_SERIF_LIGHT); }

    public TextView label(String text, float sp, int color, String font) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.create(font, Typeface.NORMAL));
        return textView;
    }

    public void shade(TextView t) {
        t.setShadowLayer(dp(UiDimensions.SPACING_6_DP), 0.0f,
                dp(UiDimensions.BORDER_THIN_DP), UiColors.TEXT_SHADOW);
    }

    public TextView pill(String text, boolean filled, int mode, Runnable onClick) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(mode == UiModes.PILL_LARGE
                ? PILL_LARGE_TEXT_SIZE_SP
                : mode == UiModes.PILL_COMPACT
                        ? PILL_COMPACT_TEXT_SIZE_SP
                        : PILL_STANDARD_TEXT_SIZE_SP);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.create(Fonts.SANS_SERIF_MEDIUM, Typeface.NORMAL));
        button.setTextColor(filled ? UiColors.PILL_TEXT_DARK : Color.WHITE);

        int horizontalPadding = mode == UiModes.PILL_LARGE
                ? PRIMARY_PILL_HORIZONTAL_PADDING_DP
                : mode == UiModes.PILL_COMPACT
                        ? COMPACT_PILL_HORIZONTAL_PADDING_DP
                        : STANDARD_PILL_HORIZONTAL_PADDING_DP;
        int verticalPadding = mode == UiModes.PILL_LARGE
                ? PRIMARY_PILL_VERTICAL_PADDING_DP
                : mode == UiModes.PILL_COMPACT
                        ? COMPACT_PILL_VERTICAL_PADDING_DP
                        : STANDARD_PILL_VERTICAL_PADDING_DP;
        button.setPadding(dp(horizontalPadding), dp(verticalPadding),
                dp(horizontalPadding), dp(verticalPadding));
        int radius = mode == UiModes.PILL_LARGE
                ? PRIMARY_PILL_CORNER_RADIUS_DP
                : STANDARD_PILL_CORNER_RADIUS_DP;
        GradientDrawable background = filled
                ? rounded(accent, radius)
                : rounded(glass, radius, glassStroke);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(UiColors.PILL_RIPPLE), background, null));
        button.setClickable(true);
        button.setOnClickListener(view -> onClick.run());
        return button;
    }

    public LinearLayout glassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(glass, CARD_CORNER_RADIUS_DP, glassStroke));
        card.setPadding(dp(UiDimensions.SPACING_28_DP), dp(UiDimensions.SPACING_24_DP), dp(UiDimensions.SPACING_28_DP), dp(UiDimensions.SPACING_24_DP));
        return card;
    }

    private void updateDots(int selectedIndex) {
        for (int index = 0; index < dots.size(); index++) {
            View dot = dots.get(index);
            boolean selected = index == selectedIndex;
            dot.setBackground(rounded(
                    selected ? Color.WHITE : UiColors.DOT_UNSELECTED,
                    SELECTED_DOT_CORNER_RADIUS_DP
            ));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            params.width = dp(selected ? SELECTED_DOT_WIDTH_DP : UNSELECTED_DOT_WIDTH_DP);
            dot.setLayoutParams(params);
        }
    }

    private void applyTheme() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int[] colors;
        if (hour >= MORNING_THEME_START_HOUR && hour < MORNING_THEME_END_HOUR) {
            colors = new int[] { UiColors.DAWN_SKY_TOP, UiColors.DAWN_SKY_BOTTOM };
        } else if (hour >= DAY_THEME_START_HOUR && hour < DAY_THEME_END_HOUR) {
            colors = new int[] { UiColors.DAY_SKY_TOP, UiColors.DAY_SKY_BOTTOM };
        } else if (hour >= EVENING_THEME_START_HOUR && hour < EVENING_THEME_END_HOUR) {
            colors = new int[] { UiColors.DUSK_SKY_TOP, UiColors.DUSK_SKY_BOTTOM };
        } else {
            colors = new int[] { UiColors.NIGHT_SKY_TOP, UiColors.NIGHT_SKY_BOTTOM };
        }
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors));
        if (hour >= MORNING_GREETING_START_HOUR && hour < MORNING_GREETING_END_HOUR) {
            homeScreen.setGreeting(getString(R.string.greeting_morning));
        } else if (hour >= DAY_GREETING_START_HOUR && hour < DAY_GREETING_END_HOUR) {
            homeScreen.setGreeting(getString(R.string.greeting_day));
        } else {
            homeScreen.setGreeting(getString(R.string.greeting_evening));
        }
    }

    private View homePage() { return homeScreen.create(); }
    private View hourlyPage() { return forecastPages.hourlyPage(); }
    private View calendarPage() { return calendarScreen.createPage(); }
    private View toolsPage() { return timerController.createToolsPage(); }
    private void loadCalendar() { calendarScreen.refresh(); }
    private void loadWeather() { weatherLoader.load(); }
    public void openForecast(View origin) { forecastPages.openForecast(origin); }
    public void openCalendarPage() { pager.setCurrentItem(CALENDAR_PAGE_INDEX); }
    public void setNextEventText(String text) { homeScreen.setNextEvent(text); }

    public void showOverlay(View content, View origin) {
        FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(UiColors.OVERLAY_BACKGROUND);
        box.setClickable(true);
        box.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));
        TextView close = pill(getString(R.string.close), false,
                UiModes.PILL_STANDARD, this::closeExpanded);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(UiDimensions.SPACING_24_DP), dp(UiDimensions.SPACING_32_DP), 0);
        box.addView(close, cp);
        FrameLayout frameLayout = root;

        frameLayout.addView(box, new FrameLayout.LayoutParams(MATCH, MATCH));
        overlay = box;
        playOpen(box, origin);
    }

    private void playOpen(View box, View origin) {
        if (origin != null) {
            int[] originPosition = new int[WINDOW_COORDINATE_COUNT];
            origin.getLocationInWindow(originPosition);
            box.setPivotX(originPosition[0] + (origin.getWidth() / 2.0f));
            box.setPivotY(originPosition[1] + (origin.getHeight() / 2.0f));
            overlayShrink = OVERLAY_ORIGIN_SCALE;
        } else {
            overlayShrink = OVERLAY_DEFAULT_SCALE;
        }
        box.setAlpha(UiValues.TRANSPARENT_ALPHA);
        box.setScaleX(overlayShrink);
        box.setScaleY(overlayShrink);
        box.animate()
                .alpha(UiValues.OPAQUE_ALPHA)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(OVERLAY_OPEN_DURATION_MILLIS)
                .setInterpolator(new DecelerateInterpolator(OVERLAY_OPEN_DECELERATION))
                .start();
        pager.animate().cancel();
        dotRow.animate().cancel();
        pager.animate()
                .alpha(UiValues.TRANSPARENT_ALPHA)
                .setDuration(PAGE_FADE_DURATION_MILLIS)
                .withEndAction(() -> { if (overlay != null) pager.setVisibility(View.INVISIBLE); })
                .start();
        dotRow.animate().alpha(UiValues.TRANSPARENT_ALPHA)
                .setDuration(PAGE_FADE_DURATION_MILLIS).start();
    }

    public void openClock(View origin) {
        dismissOverlayNow();
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour(getString(R.string.expanded_clock_format));
        clock.setFormat24Hour(getString(R.string.expanded_clock_format));
        clock.setTextSize(UiTextSizes.SIZE_140_SP);
        clock.setTextColor(Color.WHITE);
        clock.setTypeface(Typeface.create(Fonts.SANS_SERIF_THIN, android.graphics.Typeface.NORMAL));
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        TextClock date = new TextClock(this);
        date.setFormat12Hour(getString(R.string.expanded_date_format));
        date.setFormat24Hour(getString(R.string.expanded_date_format));
        date.setTextSize(UiTextSizes.SIZE_28_SP);
        date.setTextColor(softWhite);
        date.setTypeface(Typeface.create(Fonts.SANS_SERIF_LIGHT, android.graphics.Typeface.NORMAL));
        date.setGravity(Gravity.CENTER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.addView(clock, lp(MATCH, WRAP));
        LinearLayout.LayoutParams positionedLayoutParams = lp(MATCH, WRAP);
        positionedLayoutParams.topMargin = dp(UiDimensions.SPACING_12_DP);
        col.addView(date, positionedLayoutParams);
        showOverlay(col, origin);
    }

    public void openExpanded(boolean isStopwatch, View origin) {
        dismissOverlayNow();
        TimerPanel panel = timerController.createPanel(isStopwatch, true);
        List<Runnable> panelUpdaters = timerController.updaters(isStopwatch);
        panelUpdaters.add(panel.update);
        overlayUpdater = panel.update;
        overlayUpdaterList = panelUpdaters;
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(UiDimensions.SPACING_32_DP), dp(UiDimensions.SPACING_16_DP), dp(UiDimensions.SPACING_32_DP), dp(UiDimensions.SPACING_16_DP));
        center.addView(label(getString(isStopwatch ? R.string.stopwatch : R.string.timer), UiTextSizes.SIZE_20_SP, accent, Fonts.SANS_SERIF_MEDIUM));
        LinearLayout.LayoutParams pp = lp(MATCH, WRAP);
        pp.topMargin = dp(UiDimensions.SPACING_8_DP);
        center.addView(panel.view, pp);
        showOverlay(center, origin);
    }

    private void detachOverlayState() {
        List<Runnable> updaters;
        Runnable updater = overlayUpdater;
        if (updater != null && (updaters = overlayUpdaterList) != null) {
            updaters.remove(updater);
        }
        overlayUpdater = null;
        overlayUpdaterList = null;
    }

    public void dismissOverlayNow() {
        View view = overlay;
        if (view != null) {
            root.removeView(view);
        }
        overlay = null;
        detachOverlayState();
    }

    private void closeExpanded() {
        View box = overlay;
        if (box == null) return;
        overlay = null;
        detachOverlayState();
        box.animate()
                .alpha(UiValues.TRANSPARENT_ALPHA)
                .scaleX(overlayShrink)
                .scaleY(overlayShrink)
                .setDuration(OVERLAY_CLOSE_DURATION_MILLIS)
                .setInterpolator(new AccelerateInterpolator(OVERLAY_CLOSE_ACCELERATION))
                .withEndAction(() -> root.removeView(box))
                .start();
        pager.animate().cancel();
        dotRow.animate().cancel();
        pager.setVisibility(View.VISIBLE);
        dotRow.setVisibility(View.VISIBLE);
        pager.animate().alpha(UiValues.OPAQUE_ALPHA)
                .setDuration(PAGE_FADE_DURATION_MILLIS).start();
        dotRow.animate().alpha(UiValues.OPAQUE_ALPHA)
                .setDuration(PAGE_FADE_DURATION_MILLIS).start();
    }

    private void weatherMessage(String msg) {
        homeScreen.showWeatherMessage(msg);
    }

    private void displayWeather(Forecast forecast) {
        Pair<String, String> weatherDescription = weatherLabel(
                forecast.weatherCode,
                forecast.isDay
        );
        String temperature = getString(R.string.temperature_format, forecast.temperature);
        String temperatureRange = getString(
                R.string.temperature_range_format,
                forecast.highTemperature,
                forecast.lowTemperature
        );
        homeScreen.showWeather(
                weatherDescription.first,
                temperature,
                weatherDescription.second,
                temperatureRange
        );
        weatherView.setWeather(forecast.weatherCode, forecast.isDay);
        forecastPages.setForecast(forecast);
    }

    public Pair<String, String> weatherLabel(int weatherCode, boolean isDay) {
        int iconResourceId;
        int descriptionResourceId;
        switch (weatherCode) {
            case WeatherCodes.WMO_CLEAR_SKY -> {
                iconResourceId = isDay
                        ? R.string.weather_clear_icon
                        : R.string.weather_night_icon;
                descriptionResourceId = R.string.weather_clear;
            }
            case WeatherCodes.WMO_MAINLY_CLEAR, WeatherCodes.WMO_PARTLY_CLOUDY -> {
                iconResourceId = isDay
                        ? R.string.weather_partly_cloudy_icon
                        : R.string.weather_night_icon;
                descriptionResourceId = R.string.weather_sunny;
            }
            case WeatherCodes.WMO_OVERCAST -> {
                iconResourceId = R.string.weather_cloudy_icon;
                descriptionResourceId = R.string.weather_cloudy;
            }
            case WeatherCodes.WMO_FOG, WeatherCodes.WMO_DEPOSITING_RIME_FOG -> {
                iconResourceId = R.string.weather_fog_icon;
                descriptionResourceId = R.string.weather_fog;
            }
            case WeatherCodes.WMO_LIGHT_DRIZZLE, WeatherCodes.WMO_DRIZZLE_VARIANT_52, WeatherCodes.WMO_MODERATE_DRIZZLE,
                 WeatherCodes.WMO_DRIZZLE_VARIANT_54, WeatherCodes.WMO_DENSE_DRIZZLE,
                 WeatherCodes.WMO_FREEZING_DRIZZLE_LIGHT, WeatherCodes.WMO_FREEZING_DRIZZLE_DENSE -> {
                iconResourceId = R.string.weather_drizzle_icon;
                descriptionResourceId = R.string.weather_drizzle;
            }
            case WeatherCodes.WMO_SLIGHT_RAIN, WeatherCodes.WMO_LIGHT_RAIN, WeatherCodes.WMO_RAIN_VARIANT_62,
                 WeatherCodes.WMO_MODERATE_RAIN, WeatherCodes.WMO_RAIN_VARIANT_64, WeatherCodes.WMO_HEAVY_RAIN,
                 WeatherCodes.WMO_FREEZING_RAIN_LIGHT, WeatherCodes.WMO_FREEZING_RAIN_HEAVY -> {
                iconResourceId = R.string.weather_rain_icon;
                descriptionResourceId = R.string.weather_rain;
            }
            case WeatherCodes.WMO_SNOW_FALL, WeatherCodes.WMO_SNOW_VARIANT_72, WeatherCodes.WMO_MODERATE_SNOW_FALL,
                 WeatherCodes.WMO_SNOW_VARIANT_74, WeatherCodes.WMO_HEAVY_SNOW_FALL, WeatherCodes.WMO_SNOW_VARIANT_76,
                 WeatherCodes.WMO_SNOW_GRAINS -> {
                iconResourceId = R.string.weather_snow_icon;
                descriptionResourceId = R.string.weather_snow;
            }
            case WeatherCodes.WMO_RAIN_SHOWERS_SLIGHT, WeatherCodes.WMO_RAIN_SHOWERS_MODERATE,
                 WeatherCodes.WMO_RAIN_SHOWERS_VIOLENT -> {
                iconResourceId = R.string.weather_drizzle_icon;
                descriptionResourceId = R.string.weather_rain_showers;
            }
            case WeatherCodes.WMO_SNOW_SHOWERS_SLIGHT, WeatherCodes.WMO_SNOW_SHOWERS_HEAVY -> {
                iconResourceId = R.string.weather_snow_shower_icon;
                descriptionResourceId = R.string.weather_snow_showers;
            }
            case WeatherCodes.WMO_THUNDERSTORM, WeatherCodes.WMO_THUNDERSTORM_HAIL_SLIGHT,
                 WeatherCodes.WMO_THUNDERSTORM_VARIANT_97, WeatherCodes.WMO_THUNDERSTORM_VARIANT_98,
                 WeatherCodes.WMO_THUNDERSTORM_HAIL_HEAVY -> {
                iconResourceId = R.string.weather_thunderstorm_icon;
                descriptionResourceId = R.string.weather_thunderstorm;
            }
            default -> {
                iconResourceId = R.string.weather_unknown_icon;
                descriptionResourceId = R.string.weather_unknown;
            }
        }
        return new Pair<>(getString(iconResourceId), getString(descriptionResourceId));
    }

    public void showApps() {
        AppPickerDialog.show(this);
    }

}
