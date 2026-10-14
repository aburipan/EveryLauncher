package jp.aburipan.everylauncher.weather;

final class WeatherTuning {
    static final int MIN_COLOR_CHANNEL = 0;
    static final int MAX_COLOR_CHANNEL = 255;
    static final int CLOUD_PUFF_COMPONENT_COUNT = 3;
    static final int BASE_CLOUD_PUFF_COUNT = 5;
    static final int RANDOM_CLOUD_PUFF_VARIANTS = 3;
    static final int STAR_COUNT = 90;
    static final int RIPPLE_COUNT = 20;
    static final int CLOUD_COUNT_CONFIG_INDEX = 0;
    static final int CLOUD_MIN_Y_CONFIG_INDEX = 1;
    static final int CLOUD_MAX_Y_CONFIG_INDEX = 2;
    static final int CLOUD_RADIUS_NORMALIZATION = 100;
    static final int RADIAL_GRADIENT_RADIUS = 100;
    static final float INITIAL_MOON_RADIUS = 40.0f;
    static final float DEFAULT_WEATHER_INTENSITY = 0.6f;
    static final float DEFAULT_OPACITY = 1.0f;
    static final float INACTIVE_PROGRESS = -1.0f;
    static final float INITIAL_SHOOTING_STAR_DELAY_SECONDS = 5.0f;
    static final float INITIAL_LIGHTNING_DELAY_SECONDS = 2.5f;
    static final float WEATHER_CHANGE_LIGHTNING_DELAY_MIN_SECONDS = 1.5f;
    static final float WEATHER_CHANGE_LIGHTNING_DELAY_MAX_SECONDS = 4.0f;
    static final float CLOUD_SCENE_SCALE = 0.16f;
    static final float CLOUD_WIDTH_PADDING = 1.0f;
    static final float CLOUD_PUFF_HORIZONTAL_SPACING = 0.55f;
    static final float CLOUD_PUFF_HORIZONTAL_JITTER = 0.08f;
    static final float CLOUD_SPEED_MIN_DP_PER_SECOND = 6.0f;
    static final float CLOUD_SPEED_MAX_DP_PER_SECOND = 22.0f;
    static final float STAR_VERTICAL_RANGE_RATIO = 0.7f;
    static final float STAR_RADIUS_MIN_DP = 0.6f;
    static final float STAR_RADIUS_MAX_DP = 1.8f;
    static final float WEATHER_INTENSITY_LIGHT = 0.3f;
    static final float WEATHER_INTENSITY_MODERATE = 0.55f;
    static final float WEATHER_INTENSITY_HIGH = 0.8f;
    static final float WEATHER_INTENSITY_FULL = 1.0f;
    static final float STAR_PHASE_RANGE = 1.0f;

    static final float[] CLEAR_CLOUD_CONFIGURATION = { 3.0f, 0.08f, 0.45f, 0.6f, 1.0f };
    static final float[] PARTLY_CLOUDY_CONFIGURATION = { 6.0f, 0.05f, 0.45f, 0.8f, 1.3f };
    static final float[] CLOUDY_CONFIGURATION = { 9.0f, -0.02f, 0.4f, 1.1f, 1.8f };
    static final float[] FOG_CONFIGURATION = { 3.0f, 0.05f, 0.4f, 1.0f, 1.6f };
    static final float[] RAIN_CONFIGURATION = { 9.0f, -0.05f, 0.3f, 1.2f, 2.0f };
    static final float[] SNOW_CONFIGURATION = { 7.0f, -0.03f, 0.3f, 1.1f, 1.7f };
    static final float[] DEFAULT_CLOUD_CONFIGURATION = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f };
}
