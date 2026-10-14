package jp.aburipan.everylauncher.weather;

import android.graphics.Color;

final class WeatherPalette {
    static final int[] SUN_GLOW_COLORS = {
            Color.argb(210, 255, 250, 215),
            Color.argb(90, 255, 215, 120),
            Color.argb(0, 255, 200, 100)
    };
    static final int[] MOON_GLOW_COLORS = {
            Color.argb(150, 210, 225, 255),
            Color.argb(50, 170, 195, 255),
            Color.argb(0, 150, 180, 255)
    };
    static final float[] RADIAL_GRADIENT_POSITIONS = { 0.0f, 0.35f, 1.0f };
    static final float[] SCRIM_GRADIENT_POSITIONS = { 0.0f, 0.5f, 1.0f };
    static final int[] VERTICAL_SCRIM_COLORS = {
            Color.argb(80, 0, 0, 0),
            Color.argb(20, 0, 0, 0),
            Color.argb(120, 0, 0, 0)
    };
    static final int[] HORIZONTAL_SCRIM_COLORS = {
            Color.argb(95, 0, 0, 0),
            Color.argb(0, 0, 0, 0)
    };
    static final int[] FOG_GLOW_COLORS = {
            Color.argb(150, 226, 230, 235),
            Color.argb(80, 226, 230, 235),
            Color.argb(0, 226, 230, 235)
    };
    static final int CLEAR_DAY_SKY_TOP = Color.rgb(28, 96, 196);
    static final int CLEAR_DAY_SKY_BOTTOM = Color.rgb(120, 190, 242);
    static final int CLEAR_NIGHT_SKY_TOP = Color.rgb(5, 8, 24);
    static final int CLEAR_NIGHT_SKY_BOTTOM = Color.rgb(24, 36, 86);
    static final int PARTLY_CLOUDY_DAY_SKY_TOP = Color.rgb(46, 100, 176);
    static final int PARTLY_CLOUDY_DAY_SKY_BOTTOM = Color.rgb(140, 188, 224);
    static final int PARTLY_CLOUDY_NIGHT_SKY_TOP = Color.rgb(10, 16, 38);
    static final int PARTLY_CLOUDY_NIGHT_SKY_BOTTOM = Color.rgb(36, 50, 94);
    static final int CLOUDY_DAY_SKY_TOP = Color.rgb(78, 92, 112);
    static final int CLOUDY_DAY_SKY_BOTTOM = Color.rgb(142, 154, 168);
    static final int CLOUDY_NIGHT_SKY_TOP = Color.rgb(22, 26, 36);
    static final int CLOUDY_NIGHT_SKY_BOTTOM = Color.rgb(52, 58, 74);
    static final int FOG_DAY_SKY_TOP = Color.rgb(100, 110, 120);
    static final int FOG_DAY_SKY_BOTTOM = Color.rgb(160, 168, 174);
    static final int FOG_NIGHT_SKY_TOP = Color.rgb(26, 30, 36);
    static final int FOG_NIGHT_SKY_BOTTOM = Color.rgb(66, 72, 80);
    static final int RAIN_DAY_SKY_TOP = Color.rgb(40, 56, 80);
    static final int RAIN_DAY_SKY_BOTTOM = Color.rgb(84, 104, 130);
    static final int RAIN_NIGHT_SKY_TOP = Color.rgb(10, 16, 28);
    static final int RAIN_NIGHT_SKY_BOTTOM = Color.rgb(32, 44, 64);
    static final int SNOW_DAY_SKY_TOP = Color.rgb(84, 102, 132);
    static final int SNOW_DAY_SKY_BOTTOM = Color.rgb(150, 166, 190);
    static final int SNOW_NIGHT_SKY_TOP = Color.rgb(26, 36, 60);
    static final int SNOW_NIGHT_SKY_BOTTOM = Color.rgb(80, 96, 124);
    static final int STORM_DAY_SKY_TOP = Color.rgb(26, 22, 48);
    static final int STORM_DAY_SKY_BOTTOM = Color.rgb(64, 56, 92);
    static final int STORM_NIGHT_SKY_TOP = Color.rgb(8, 6, 20);
    static final int STORM_NIGHT_SKY_BOTTOM = Color.rgb(36, 30, 60);
    static final int[] CLEAR_DAY_CLOUD_TINT = { 255, 255, 255, 140 };
    static final int[] CLEAR_NIGHT_CLOUD_TINT = { 150, 165, 205, 70 };
    static final int[] CLOUDY_DAY_CLOUD_TINT = { 236, 241, 246, 190 };
    static final int[] CLOUDY_NIGHT_CLOUD_TINT = { 112, 122, 148, 130 };
    static final int[] RAIN_DAY_CLOUD_TINT = { 108, 120, 136, 215 };
    static final int[] RAIN_NIGHT_CLOUD_TINT = { 58, 66, 86, 195 };
    static final int[] SNOW_DAY_CLOUD_TINT = { 224, 231, 241, 190 };
    static final int[] SNOW_NIGHT_CLOUD_TINT = { 118, 130, 158, 140 };
    static final int[] STORM_DAY_CLOUD_TINT = { 66, 68, 90, 230 };
    static final int[] STORM_NIGHT_CLOUD_TINT = { 34, 36, 56, 215 };
    static final int[] DEFAULT_CLOUD_TINT = { 226, 230, 235, 110 };
    static final int NIGHT_FILTER_MULTIPLY = Color.rgb(70, 84, 132);
    static final int NIGHT_FILTER_ADD = Color.rgb(0, 0, 6);
    static final int MOON_CORE_COLOR = Color.rgb(250, 248, 230);
    static final int STAR_COLOR = Color.WHITE;
    static final int RAIN_STREAK_COLOR = Color.rgb(200, 220, 255);
    static final int RAIN_RIPPLE_COLOR = Color.rgb(210, 225, 255);
    static final int LIGHTNING_FLASH_COLOR = Color.rgb(235, 240, 255);
    static final int LIGHTNING_OUTER_BOLT_COLOR = Color.rgb(200, 215, 255);
    static final int LIGHTNING_CORE_BOLT_COLOR = Color.WHITE;
}
