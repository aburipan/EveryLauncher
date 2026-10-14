package jp.aburipan.everylauncher.weather;

import static jp.aburipan.everylauncher.weather.WeatherCodes.*;
import static jp.aburipan.everylauncher.weather.WeatherPalette.*;
import static jp.aburipan.everylauncher.weather.WeatherTuning.*;
import static jp.aburipan.everylauncher.weather.WeatherTypes.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import jp.aburipan.everylauncher.AppConstants;
import jp.aburipan.everylauncher.EveryLauncher;

@SuppressWarnings("NullableProblems")
public final class WeatherView extends View {
    private static final String WEATHER_DIRECTORY = "weather";
    private static final String EXTERNAL_IMAGE_PREFIX = "ext:";
    private static final String ASSET_IMAGE_PREFIX = "asset:";
    private static final String IMAGE_STAMP_SEPARATOR = ":";
    private static final String FILE_EXTENSION_SEPARATOR = ".";
    private static final String CLEAR_DAY_IMAGE_NAME = "clear_day";
    private static final String CLEAR_NIGHT_IMAGE_NAME = "clear_night";
    private static final String PARTLY_CLOUDY_IMAGE_NAME = "partly";
    private static final String CLOUDY_IMAGE_NAME = "cloudy";
    private static final String FOG_IMAGE_NAME = "fog";
    private static final String RAIN_IMAGE_NAME = "rain";
    private static final String SNOW_IMAGE_NAME = "snow";
    private static final String STORM_IMAGE_NAME = "storm";
    private static final String JPEG_EXTENSION = "jpg";
    private static final String JPG_EXTENSION = "jpeg";
    private static final String PNG_EXTENSION = "png";
    private static final String WEBP_EXTENSION = "webp";

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Executor backgroundExecutor;
    private static int coerceIn(int value) { //noinspection MathClampMigration
        return Math.max(MIN_COLOR_CHANNEL,
                Math.min(MAX_COLOR_CHANNEL, value)); }
    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
    private static float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            result[valueIndex] = values.get(valueIndex);
        }
        return result;
    }
    private static void closeFinally(Closeable value, Throwable cause) throws IOException {
        if (value == null) return;
        if (cause == null) value.close();
        else try { value.close(); } catch (Throwable closeError) { cause.addSuppressed(closeError); }
    }

    private float[] lightningBolt;
    private final Paint cloudPaint;
    private float cloudScaleUnit;
    private final ArrayList<CloudLayer> clouds;
    private boolean day;
    private final float dp;
    private final List<String> exts;
    private float[] fogAlpha;
    private float[] fogPhase;
    private float[] fogRadius;
    private float[] fogSpeed;
    private float[] fogX;
    private float[] fogY;
    private float fade;
    private float flashProgress;
    private float flashDelay;
    private final Paint fogPaint;
    private final ArrayList<FogLayer> fogs;
    private final Paint glowPaint;
    private float intensity;
    private long previousFrameTimeNanos;
    private final Paint line;
    private int imageLoadToken;
    private final Path moonCut;
    private final RadialGradient moonGlow;
    private final Path moonPath;
    private float moonRadius;
    private final LightingColorFilter nightFilter;
    private Bitmap photo;
    private boolean photoIsDark;
    private float photoOpacity;
    private Bitmap previousPhoto;
    private boolean previousPhotoIsDark;
    private final Matrix photoMatrix;
    private final Paint photoPaint;
    private String photoCacheKey;
    private int rainCount;
    private float[] rainAlpha;
    private final RectF rect;
    private final float[] rippleAge;
    private float rippleTimer;
    private final float[] rippleX;
    private final float[] rippleY;
    private float[] rainLength;
    private final Random rnd;
    private float[] rainSpeed;
    private float[] rainWidth;
    private float[] rainX;
    private float[] rainY;
    private Shader horizontalScrim;
    private final Paint scrimPaint;
    private Shader verticalScrim;
    private Shader previousShader;
    private Shader currentShader;
    private float shootProgress;
    private float shootDelay;
    private float shootingStarX;
    private float shootingStarY;
    private int[] previousSkyColors;
    private final Paint skyPaint;
    private int[] currentSkyColors;
    private int snowCount;
    private final Paint solid;
    private float[] starPhase;
    private float[] starRadius;
    private float[] starX;
    private float[] starY;
    private final RadialGradient sunGlow;
    private float time;
    private int type;

    public WeatherView(Context context) {
        super(context);
        backgroundExecutor = ((EveryLauncher) context.getApplicationContext()).backgroundExecutor();
        dp = getResources().getDisplayMetrics().density;
        rnd = new Random();
        day = true;
        intensity = DEFAULT_WEATHER_INTENSITY;
        fade = DEFAULT_OPACITY;
        skyPaint = new Paint();
        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        solid = new Paint(Paint.ANTI_ALIAS_FLAG);
        line = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();
        sunGlow = new RadialGradient(
                0.0f,
                0.0f,
                RADIAL_GRADIENT_RADIUS,
                SUN_GLOW_COLORS,
                RADIAL_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP
        );
        moonGlow = new RadialGradient(
                0.0f,
                0.0f,
                RADIAL_GRADIENT_RADIUS,
                MOON_GLOW_COLORS,
                RADIAL_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP
        );
        clouds = new ArrayList<>();
        fogs = new ArrayList<>();
        cloudScaleUnit = CLOUD_RADIUS_NORMALIZATION;
        starX = new float[0];
        starY = new float[0];
        starRadius = new float[0];
        starPhase = new float[0];
        shootProgress = INACTIVE_PROGRESS;
        shootDelay = INITIAL_SHOOTING_STAR_DELAY_SECONDS;
        moonPath = new Path();
        moonCut = new Path();
        moonRadius = INITIAL_MOON_RADIUS;
        rainX = new float[0];
        rainY = new float[0];
        rainSpeed = new float[0];
        rainLength = new float[0];
        rainWidth = new float[0];
        rainAlpha = new float[0];
        rippleX = new float[RIPPLE_COUNT];
        rippleY = new float[RIPPLE_COUNT];
        float[] initialRippleAges = new float[RIPPLE_COUNT];
        Arrays.fill(initialRippleAges, DEFAULT_OPACITY);
        rippleAge = initialRippleAges;
        fogX = new float[0];
        fogY = new float[0];
        fogSpeed = new float[0];
        fogRadius = new float[0];
        fogPhase = new float[0];
        fogAlpha = new float[0];
        lightningBolt = new float[0];
        flashProgress = INACTIVE_PROGRESS;
        flashDelay = INITIAL_LIGHTNING_DELAY_SECONDS;
        photoOpacity = DEFAULT_OPACITY;
        photoCacheKey = AppConstants.EMPTY_STRING;
        photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        scrimPaint = new Paint();
        photoMatrix = new Matrix();
        nightFilter = new LightingColorFilter(
                NIGHT_FILTER_MULTIPLY,
                NIGHT_FILTER_ADD
        );
        exts = Arrays.asList(JPEG_EXTENSION, JPG_EXTENSION, PNG_EXTENSION, WEBP_EXTENSION);
    }

    private float rf(float minimum, float maximum) {
        return (rnd.nextFloat() * (maximum - minimum)) + minimum;
    }

    public void setWeather(int code, boolean isDay) {
        int weatherType = switch (code) {
            case WMO_CLEAR_SKY, WMO_MAINLY_CLEAR -> CLEAR;
            case WMO_PARTLY_CLOUDY -> PARTLY_CLOUDY;
            case WMO_OVERCAST -> CLOUDY;
            case WMO_FOG, WMO_DEPOSITING_RIME_FOG -> FOG;
            case WMO_LIGHT_DRIZZLE, WMO_DRIZZLE_VARIANT_52, WMO_MODERATE_DRIZZLE,
                    WMO_DRIZZLE_VARIANT_54, WMO_DENSE_DRIZZLE,
                    WMO_FREEZING_DRIZZLE_LIGHT, WMO_FREEZING_DRIZZLE_DENSE,
                    WMO_DRIZZLE_VARIANT_58, WMO_DRIZZLE_VARIANT_59,
                    WMO_SLIGHT_RAIN, WMO_LIGHT_RAIN, WMO_RAIN_VARIANT_62,
                    WMO_MODERATE_RAIN, WMO_RAIN_VARIANT_64, WMO_HEAVY_RAIN,
                    WMO_FREEZING_RAIN_LIGHT, WMO_FREEZING_RAIN_HEAVY,
                    WMO_RAIN_SHOWERS_SLIGHT, WMO_RAIN_SHOWERS_MODERATE,
                    WMO_RAIN_SHOWERS_VIOLENT -> RAIN;
            case WMO_SNOW_FALL, WMO_SNOW_VARIANT_72, WMO_MODERATE_SNOW_FALL,
                    WMO_SNOW_VARIANT_74, WMO_HEAVY_SNOW_FALL, WMO_SNOW_VARIANT_76,
                    WMO_SNOW_GRAINS, WMO_SNOW_SHOWERS_SLIGHT, WMO_SNOW_SHOWERS_HEAVY -> SNOW;
            case WMO_THUNDERSTORM, WMO_THUNDERSTORM_HAIL_SLIGHT,
                    WMO_THUNDERSTORM_VARIANT_97, WMO_THUNDERSTORM_VARIANT_98,
                    WMO_THUNDERSTORM_HAIL_HEAVY -> STORM;
            default -> CLOUDY;
        };
        float weatherIntensity = switch (code) {
            case WMO_LIGHT_DRIZZLE, WMO_FREEZING_DRIZZLE_LIGHT, WMO_SNOW_FALL,
                    WMO_SNOW_SHOWERS_SLIGHT -> WEATHER_INTENSITY_LIGHT;
            case WMO_MODERATE_DRIZZLE, WMO_LIGHT_RAIN, WMO_FREEZING_RAIN_LIGHT,
                    WMO_MODERATE_SNOW_FALL, WMO_RAIN_SHOWERS_SLIGHT -> WEATHER_INTENSITY_MODERATE;
            case WMO_DENSE_DRIZZLE, WMO_FREEZING_DRIZZLE_DENSE, WMO_MAINLY_CLEAR,
                    WMO_SNOW_GRAINS, WMO_RAIN_SHOWERS_MODERATE, WMO_THUNDERSTORM -> WEATHER_INTENSITY_HIGH;
            default -> WEATHER_INTENSITY_FULL;
        };
        if (weatherType == type && isDay == day && weatherIntensity == intensity) {
            return;
        }
        previousSkyColors = currentSkyColors;
        type = weatherType;
        day = isDay;
        intensity = weatherIntensity;
        currentSkyColors = skyFor(weatherType, isDay);
        fade = 0.0f;
        flashProgress = INACTIVE_PROGRESS;
        flashDelay = rf(WEATHER_CHANGE_LIGHTNING_DELAY_MIN_SECONDS,
                WEATHER_CHANGE_LIGHTNING_DELAY_MAX_SECONDS);
        buildScene();
        startLoad();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        buildScene();
    }

    private static int[] createSkyGradient(boolean isDay, int dayTopColor, int dayBottomColor,
                                           int nightTopColor, int nightBottomColor) {
        return isDay
                ? new int[] { dayTopColor, dayBottomColor }
                : new int[] { nightTopColor, nightBottomColor };
    }

    private int[] skyFor(int weatherType, boolean isDay) {
        return switch (weatherType) {
            case CLEAR -> createSkyGradient(isDay,
                    CLEAR_DAY_SKY_TOP, CLEAR_DAY_SKY_BOTTOM,
                    CLEAR_NIGHT_SKY_TOP, CLEAR_NIGHT_SKY_BOTTOM);
            case PARTLY_CLOUDY -> createSkyGradient(isDay,
                    PARTLY_CLOUDY_DAY_SKY_TOP,
                    PARTLY_CLOUDY_DAY_SKY_BOTTOM,
                    PARTLY_CLOUDY_NIGHT_SKY_TOP,
                    PARTLY_CLOUDY_NIGHT_SKY_BOTTOM);
            case CLOUDY -> createSkyGradient(isDay,
                    CLOUDY_DAY_SKY_TOP, CLOUDY_DAY_SKY_BOTTOM,
                    CLOUDY_NIGHT_SKY_TOP, CLOUDY_NIGHT_SKY_BOTTOM);
            case FOG -> createSkyGradient(isDay,
                    FOG_DAY_SKY_TOP, FOG_DAY_SKY_BOTTOM,
                    FOG_NIGHT_SKY_TOP, FOG_NIGHT_SKY_BOTTOM);
            case RAIN -> createSkyGradient(isDay,
                    RAIN_DAY_SKY_TOP, RAIN_DAY_SKY_BOTTOM,
                    RAIN_NIGHT_SKY_TOP, RAIN_NIGHT_SKY_BOTTOM);
            case SNOW -> createSkyGradient(isDay,
                    SNOW_DAY_SKY_TOP, SNOW_DAY_SKY_BOTTOM,
                    SNOW_NIGHT_SKY_TOP, SNOW_NIGHT_SKY_BOTTOM);
            default -> createSkyGradient(isDay,
                    STORM_DAY_SKY_TOP, STORM_DAY_SKY_BOTTOM,
                    STORM_NIGHT_SKY_TOP, STORM_NIGHT_SKY_BOTTOM);
        };
    }

    private int[] cloudTint() {
        return switch (type) {
            case CLEAR, PARTLY_CLOUDY -> day
                    ? CLEAR_DAY_CLOUD_TINT
                    : CLEAR_NIGHT_CLOUD_TINT;
            case CLOUDY -> day
                    ? CLOUDY_DAY_CLOUD_TINT
                    : CLOUDY_NIGHT_CLOUD_TINT;
            case RAIN -> day
                    ? RAIN_DAY_CLOUD_TINT
                    : RAIN_NIGHT_CLOUD_TINT;
            case SNOW -> day
                    ? SNOW_DAY_CLOUD_TINT
                    : SNOW_NIGHT_CLOUD_TINT;
            case STORM -> day
                    ? STORM_DAY_CLOUD_TINT
                    : STORM_NIGHT_CLOUD_TINT;
            default -> DEFAULT_CLOUD_TINT;
        };
    }

    private void buildScene() {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0.0f || height <= 0.0f || type == NONE) {
            return;
        }
        if (currentSkyColors != null) {
            currentShader = new LinearGradient(
                    0.0f, 0.0f, 0.0f, height,
                    currentSkyColors[0], currentSkyColors[1],
                    Shader.TileMode.CLAMP
            );
        }
        verticalScrim = new LinearGradient(
                0.0f, 0.0f, 0.0f, height,
                VERTICAL_SCRIM_COLORS,
                SCRIM_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP
        );
        horizontalScrim = new LinearGradient(
                0.0f, 0.0f, width * 0.65f, 0.0f,
                HORIZONTAL_SCRIM_COLORS,
                null,
                Shader.TileMode.CLAMP
        );
        previousShader = previousSkyColors != null
                ? new LinearGradient(
                        0.0f, 0.0f, 0.0f, height,
                        previousSkyColors[0], previousSkyColors[1],
                        Shader.TileMode.CLAMP
                )
                : null;
        int[] tint = cloudTint();
        cloudPaint.setShader(new RadialGradient(
                0.0f,
                0.0f,
                100.0f,
                new int[] {
                        Color.argb(tint[3], tint[0], tint[1], tint[2]),
                        Color.argb((tint[3] * 3) / 4, tint[0], tint[1], tint[2]),
                        Color.argb(0, tint[0], tint[1], tint[2])
                },
                SCRIM_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP
        ));
        fogPaint.setShader(new RadialGradient(
                0.0f,
                0.0f,
                100.0f,
                FOG_GLOW_COLORS,
                SCRIM_GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP
        ));

        clouds.clear();
        cloudScaleUnit = Math.min(width, height) * CLOUD_SCENE_SCALE;
        float[] cloudConfig = switch (type) {
            case CLEAR -> CLEAR_CLOUD_CONFIGURATION;
            case PARTLY_CLOUDY -> PARTLY_CLOUDY_CONFIGURATION;
            case CLOUDY -> CLOUDY_CONFIGURATION;
            case FOG -> FOG_CONFIGURATION;
            case RAIN, STORM -> RAIN_CONFIGURATION;
            case SNOW -> SNOW_CONFIGURATION;
            default -> DEFAULT_CLOUD_CONFIGURATION;
        };
        int cloudCount = (int) cloudConfig[CLOUD_COUNT_CONFIG_INDEX];
        for (int cloudIndex = 0; cloudIndex < cloudCount; cloudIndex++) {
            float scale = rf(cloudConfig[3], cloudConfig[4]);
            int puffCount = rnd.nextInt(RANDOM_CLOUD_PUFF_VARIANTS)
                    + BASE_CLOUD_PUFF_COUNT;
            float cloudMiddle = (puffCount - 1) / 2.0f;
            float[] puffs = new float[puffCount * CLOUD_PUFF_COMPONENT_COUNT];
            for (int puffIndex = 0; puffIndex < puffCount; puffIndex++) {
                float edge = Math.abs(puffIndex - cloudMiddle) / cloudMiddle;
                puffs[puffIndex * 3] = ((puffIndex - cloudMiddle) * 0.55f)
                        + rf(-0.08f, CLOUD_PUFF_HORIZONTAL_JITTER);
                puffs[(puffIndex * 3) + 1] = ((1.0f - edge) * -0.22f)
                        + rf(-0.06f, 0.06f);
                puffs[(puffIndex * 3) + 2] = ((1.0f - edge) * 0.4f) + 0.55f
                        + rf(-0.05f, 0.1f);
            }
            float cloudWidth = ((puffCount * CLOUD_PUFF_HORIZONTAL_SPACING)
                    + CLOUD_WIDTH_PADDING) * cloudScaleUnit * scale;
            clouds.add(new CloudLayer(rf(-cloudWidth, width + cloudWidth),
                    height * rf(cloudConfig[CLOUD_MIN_Y_CONFIG_INDEX],
                            cloudConfig[CLOUD_MAX_Y_CONFIG_INDEX]), scale,
                    rf(CLOUD_SPEED_MIN_DP_PER_SECOND,
                            CLOUD_SPEED_MAX_DP_PER_SECOND) * dp * scale, cloudWidth, puffs));
        }

        fogs.clear();
        if (type == FOG) {
            for (int fogLayerIndex = 0; fogLayerIndex < 6; fogLayerIndex++) {
                fogs.add(new FogLayer(rf((-width) * 0.3f, width),
                        height * rf(0.25f, 0.95f), (rf(0.35f, 0.6f) * width) / 100.0f,
                        (rf(0.08f, 0.16f) * height) / 100.0f, rf(8.0f, 20.0f) * dp));
            }
        }
        starX = new float[STAR_COUNT];
        for (int starIndex = 0; starIndex < starX.length; starIndex++) {
            starX[starIndex] = rf(0.0f, width);
        }
        starY = new float[STAR_COUNT];
        for (int starIndex = 0; starIndex < starY.length; starIndex++) {
            starY[starIndex] = rf(0.0f, STAR_VERTICAL_RANGE_RATIO * height);
        }
        starRadius = new float[STAR_COUNT];
        for (int starIndex = 0; starIndex < starRadius.length; starIndex++) {
            starRadius[starIndex] = rf(STAR_RADIUS_MIN_DP, STAR_RADIUS_MAX_DP) * dp;
        }
        starPhase = new float[STAR_COUNT];
        IntStream.range(0, starPhase.length).forEach(starIndex -> starPhase[starIndex] = rf(0.0f, STAR_PHASE_RANGE));

        moonRadius = Math.min(width, height) * 0.07f;
        moonPath.reset();
        moonPath.addCircle(0.0f, 0.0f, moonRadius, Path.Direction.CW);
        moonCut.reset();
        moonCut.addCircle(moonRadius * 0.5f, (-moonRadius) * 0.18f,
                moonRadius * 0.88f, Path.Direction.CW);
        moonPath.op(moonCut, Path.Op.DIFFERENCE);
        shootProgress = -1.0f;
        shootDelay = rf(3.0f, 8.0f);

        rainCount = (type == RAIN || type == STORM)
                ? (int) (50.0f + (200.0f * intensity))
                : 0;
        float particleSpeed = (intensity * 0.4f) + 0.6f;
        rainX = new float[rainCount];
        rainY = new float[rainCount];
        rainSpeed = new float[rainCount];
        rainLength = new float[rainCount];
        rainWidth = new float[rainCount];
        rainAlpha = new float[rainCount];
        for (int rainIndex = 0; rainIndex < rainCount; rainIndex++) {
            float depth = rnd.nextFloat();
            rainSpeed[rainIndex] = ((900.0f * depth) + 800.0f) * dp * particleSpeed;
            rainLength[rainIndex] = ((24.0f * depth) + 14.0f) * dp * particleSpeed;
            rainWidth[rainIndex] = (1.4f * depth + 0.8f) * dp;
            rainAlpha[rainIndex] = (130.0f * depth) + 60.0f;
            rainX[rainIndex] = rf(0.0f, 1.3f * width);
            rainY[rainIndex] = rf(0.0f, height);
        }
        Arrays.fill(rippleAge, 1.0f);

        snowCount = type == SNOW ? (int) (45.0f + (150.0f * intensity)) : 0;
        fogX = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogX[snowIndex] = rf(0.0f, width);
        }
        fogY = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogY[snowIndex] = rf(0.0f, height);
        }
        fogRadius = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogRadius[snowIndex] = rf(1.2f, 3.8f) * dp;
        }
        fogSpeed = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogSpeed[snowIndex] = (((fogRadius[snowIndex] / dp) * 14.0f) + 18.0f) * dp;
        }
        fogPhase = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogPhase[snowIndex] = rf(0.0f, 6.28f);
        }
        fogAlpha = new float[snowCount];
        for (int snowIndex = 0; snowIndex < snowCount; snowIndex++) {
            fogAlpha[snowIndex] = rf(140.0f, 235.0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (type == NONE) {
            return;
        }
        long now = System.nanoTime();
        float deltaSeconds = previousFrameTimeNanos == 0
                ? 0.016f
                : (now - previousFrameTimeNanos) / 1.0E9f;
        previousFrameTimeNanos = now;
        if (deltaSeconds > 0.05f) {
            deltaSeconds = 0.05f;
        }
        time += deltaSeconds;
        if (fade < 1.0f) {
            fade = Math.min(1.0f, fade + deltaSeconds);
        }
        if (photoOpacity < 1.0f) {
            photoOpacity = Math.min(1.0f, photoOpacity + (deltaSeconds / 1.2f));
        }
        drawScene(canvas, deltaSeconds);
        boolean animated = type == RAIN || type == SNOW || type == STORM
                || (type == CLEAR && !day);
        if (photo == null || photoOpacity < 1.0f || animated) {
            postInvalidateOnAnimation();
        } else {
            postInvalidateDelayed(50L);
        }
    }

    private void drawScene(Canvas canvas, float deltaSeconds) {
        Bitmap previousPhotoBitmap;
        Bitmap currentPhotoBitmap = photo;
        boolean needsProceduralBackground = currentPhotoBitmap == null
                || (photoOpacity < 1.0f && previousPhoto == null);
        if (needsProceduralBackground) {
            drawProcedural(canvas, deltaSeconds);
        }
        if (currentPhotoBitmap != null) {
            if (photoOpacity < 1.0f && (previousPhotoBitmap = previousPhoto) != null) {
                drawPhoto(canvas, previousPhotoBitmap, 255, previousPhotoIsDark);
            }
            drawPhoto(canvas, currentPhotoBitmap, (int) (255.0f * photoOpacity), photoIsDark);
            drawScrim(canvas, photoOpacity);
        }
        drawParticles(canvas, deltaSeconds, currentPhotoBitmap != null);
    }

    private String imageName(int weatherType, boolean isDay) {
        return switch (weatherType) {
            case CLEAR -> isDay ? CLEAR_DAY_IMAGE_NAME : CLEAR_NIGHT_IMAGE_NAME;
            case PARTLY_CLOUDY -> PARTLY_CLOUDY_IMAGE_NAME;
            case CLOUDY -> CLOUDY_IMAGE_NAME;
            case FOG -> FOG_IMAGE_NAME;
            case RAIN -> RAIN_IMAGE_NAME;
            case SNOW -> SNOW_IMAGE_NAME;
            default -> STORM_IMAGE_NAME;
        };
    }

    private String stampFor(String name) {
        File dir = getContext().getExternalFilesDir(WEATHER_DIRECTORY);
        if (dir != null) {
            for (String ext : exts) {
                File imageFile = new File(dir, name + FILE_EXTENSION_SEPARATOR + ext);
                if (imageFile.exists()) {
                    return EXTERNAL_IMAGE_PREFIX + imageFile.getName()
                            + IMAGE_STAMP_SEPARATOR + imageFile.lastModified();
                }
            }
        }
        try {
            String[] list = getContext().getAssets().list(WEATHER_DIRECTORY);
            if (list != null) {
                for (String extension : exts) {
                    if (Arrays.asList(list).contains(name + FILE_EXTENSION_SEPARATOR + extension)) {
                        return ASSET_IMAGE_PREFIX + name + FILE_EXTENSION_SEPARATOR + extension;
                    }
                }
                return AppConstants.EMPTY_STRING;
            }
            return AppConstants.EMPTY_STRING;
        } catch (Exception e2) {
            return AppConstants.EMPTY_STRING;
        }
    }

    private static Bitmap decodeStamp(Context context, String stamp, int targetSize) {
        if (stamp.isEmpty()) {
            return null;
        }
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            InputStream boundsInput = openWeatherImage(context, stamp);
            if (boundsInput != null) {
                try {
                    BitmapFactory.decodeStream(boundsInput, null, boundsOptions);
                    closeFinally(boundsInput, null);
                } catch (Throwable error) {
                    try {
                        throw error;
                    } catch (Throwable closeError) {
                        closeFinally(boundsInput, error);
                        throw closeError;
                    }
                }
            }
            int sampleSize = 1;
            while (boundsOptions.outWidth / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2;
            }
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            InputStream imageInput = openWeatherImage(context, stamp);
            if (imageInput == null) {
                return null;
            }
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(imageInput, null, decodeOptions);
                closeFinally(imageInput, null);
                return bitmap;
            } catch (Throwable error) {
                try {
                    throw error;
                } catch (Throwable closeError) {
                    closeFinally(imageInput, error);
                    throw closeError;
                }
            }
        } catch (Throwable error) {
            return null;
        }
    }

    private static InputStream openWeatherImage(Context context, String stamp)
            throws java.io.IOException {
        if (!stamp.startsWith(EXTERNAL_IMAGE_PREFIX)) {
            return context.getAssets().open(
                    WEATHER_DIRECTORY + "/" + stamp.substring(ASSET_IMAGE_PREFIX.length())
            );
        }
        File dir = context.getExternalFilesDir(WEATHER_DIRECTORY);
        return dir != null
                ? new FileInputStream(
                        new File(dir, stamp.substring(
                                EXTERNAL_IMAGE_PREFIX.length(),
                                stamp.lastIndexOf(IMAGE_STAMP_SEPARATOR)
                        ))
                )
                : null;
    }

    private void startLoad() {
        String name = imageName(type, day);
        final String stamp = stampFor(name);
        final boolean dark = !day && !java.util.Objects.equals(name, CLEAR_NIGHT_IMAGE_NAME);
        if (java.util.Objects.equals(stamp, photoCacheKey) && photo != null) {
            photoIsDark = dark;
            return;
        }
        photoCacheKey = stamp;
        imageLoadToken++;
        final int token = imageLoadToken;
        Context applicationContext = getContext().getApplicationContext();
        int targetSize = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        if (stamp.isEmpty()) {
            photo = null;
            previousPhoto = null;
        } else {
            backgroundExecutor.execute(() -> {
                Bitmap bitmap = decodeStamp(applicationContext, stamp, targetSize);
                mainThreadHandler.post(() -> applyLoadedImage(bitmap, dark, token));
            });
        }
    }

    private void applyLoadedImage(Bitmap bitmap, boolean dark, int token) {
        if (token != imageLoadToken) {
            if (bitmap != null) bitmap.recycle();
            return;
        }
        if (bitmap != null) {
            previousPhoto = photo;
            previousPhotoIsDark = photoIsDark;
            photo = bitmap;
            photoIsDark = dark;
            photoOpacity = 0f;
        } else {
            photo = null;
            previousPhoto = null;
            photoCacheKey = AppConstants.EMPTY_STRING;
        }
        invalidate();
    }

    public void refreshImages() {
        if (type != NONE && !java.util.Objects.equals(stampFor(imageName(type, day)), photoCacheKey)) {
            startLoad();
        }
    }

    private void drawPhoto(Canvas canvas, Bitmap bitmap, int alpha, boolean dark) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float baseScale = Math.max(viewWidth / bitmap.getWidth(), viewHeight / bitmap.getHeight());
        float imageScale = ((((float) Math.sin(time * 0.05f)) * 0.06f) + 1.12f) * baseScale;
        float horizontalOverflow = Math.max(0.0f, (bitmap.getWidth() * imageScale) - viewWidth);
        float verticalOverflow = Math.max(0.0f, (bitmap.getHeight() * imageScale) - viewHeight);
        float horizontalPosition = (((float) Math.sin(time * 0.04f)) * 0.5f) + 0.5f;
        float verticalPosition = (((float) Math.sin((time * 0.031f) + 1.0f)) * 0.5f) + 0.5f;
        photoMatrix.reset();
        photoMatrix.postScale(imageScale, imageScale);
        photoMatrix.postTranslate(
                -horizontalOverflow * horizontalPosition,
                -verticalOverflow * verticalPosition
        );
        photoPaint.setAlpha(alpha);
        photoPaint.setColorFilter(dark ? nightFilter : null);
        canvas.drawBitmap(bitmap, photoMatrix, photoPaint);
    }

    private void drawScrim(Canvas canvas, float alpha) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        scrimPaint.setAlpha((int) (255.0f * alpha));
        scrimPaint.setShader(verticalScrim);
        canvas.drawRect(0.0f, 0.0f, viewWidth, viewHeight, scrimPaint);
        scrimPaint.setShader(horizontalScrim);
        scrimPaint.setAlpha((int) (255.0f * alpha));
        canvas.drawRect(0.0f, 0.0f, viewWidth, viewHeight, scrimPaint);
    }

    private void drawProcedural(Canvas canvas, float deltaSeconds) {
        float alpha = fade;
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        if (fade < 1.0f) {
            Shader shader = previousShader;
            if (shader != null) {
                skyPaint.setShader(shader);
                skyPaint.setAlpha(255);
                canvas.drawRect(0.0f, 0.0f, viewWidth, viewHeight, skyPaint);
            }
            skyPaint.setShader(currentShader);
            skyPaint.setAlpha((int) (255.0f * alpha));
        } else {
            skyPaint.setShader(currentShader);
            skyPaint.setAlpha(255);
        }
        canvas.drawRect(0.0f, 0.0f, viewWidth, viewHeight, skyPaint);
        switch (type) {
            case CLEAR:
                if (day) {
                    drawSun(canvas, alpha);
                } else {
                    drawStars(canvas, alpha);
                    drawMoon(canvas, alpha);
                }
                drawClouds(canvas, deltaSeconds, alpha);
                break;
            case PARTLY_CLOUDY:
                if (day) {
                    drawSun(canvas, 0.8f * alpha);
                } else {
                    drawStars(canvas, 0.6f * alpha);
                    drawMoon(canvas, 0.8f * alpha);
                }
                drawClouds(canvas, deltaSeconds, alpha);
                break;
            case CLOUDY:
            case FOG:
                drawClouds(canvas, deltaSeconds, alpha);
                drawFog(canvas, deltaSeconds, alpha);
                break;
            default:
                drawClouds(canvas, deltaSeconds, alpha);
                break;
        }
    }

    private void drawParticles(Canvas canvas, float deltaSeconds, boolean onPhoto) {
        float alpha = onPhoto ? 0.65f : fade;
        switch (type) {
            case CLEAR:
                if (!day) {
                    drawShooting(canvas, deltaSeconds, onPhoto ? 1.0f : fade);
                }
                break;
            case RAIN:
                drawRain(canvas, deltaSeconds, alpha, !onPhoto);
                break;
            case SNOW:
                drawSnow(canvas, deltaSeconds, onPhoto ? 0.9f : fade);
                break;
            case STORM:
                drawRain(canvas, deltaSeconds, alpha, !onPhoto);
                drawLightning(canvas, deltaSeconds, onPhoto ? 1.0f : fade, !onPhoto);
                break;
        }
    }

    private void drawClouds(Canvas canvas, float deltaSeconds, float alpha) {
        float viewWidth = getWidth();
        cloudPaint.setAlpha((int) (255.0f * alpha));
        for (CloudLayer cloud : clouds) {
            cloud.positionX += cloud.speed * deltaSeconds;
            if (cloud.positionX - cloud.width > viewWidth) {
                cloud.positionX = -cloud.width;
            }

            int puffCount = cloud.puffs.length / 3;
            for (int puffIndex = 0; puffIndex < puffCount; puffIndex++) {
                int puffOffset = puffIndex * 3;
                float centerX = cloud.positionX
                        + cloud.puffs[puffOffset] * cloudScaleUnit * cloud.scale;
                float centerY = cloud.positionY
                        + cloud.puffs[puffOffset + 1] * cloudScaleUnit * cloud.scale;
                float radius = cloud.puffs[puffOffset + 2] * cloudScaleUnit * cloud.scale / 100.0f;
                canvas.save();
                canvas.translate(centerX, centerY);
                canvas.scale(radius, 0.75f * radius);
                canvas.drawCircle(0.0f, 0.0f, 100.0f, cloudPaint);
                canvas.restore();
            }
        }
    }

    private void drawSun(Canvas canvas, float alpha) {
        float radius = Math.min(getWidth(), getHeight()) * 0.085f;
        float centerX = getWidth() * 0.8f;
        float centerY = getHeight() * 0.18f;
        float pulse = (((float) Math.sin(time * 0.9f)) * 0.04f) + 1.0f;
        glowPaint.setShader(sunGlow);
        glowPaint.setAlpha((int) (255.0f * alpha));
        canvas.save();
        canvas.translate(centerX, centerY);
        float glowScale = ((6.0f * radius) * pulse) / 100.0f;
        canvas.scale(glowScale, glowScale);
        canvas.drawCircle(0.0f, 0.0f, 100.0f, glowPaint);
        canvas.restore();
        canvas.save();
        canvas.translate(centerX, centerY);
        float coreScale = (2.0f * radius) / 100.0f;
        canvas.scale(coreScale, coreScale);
        canvas.drawCircle(0.0f, 0.0f, 100.0f, glowPaint);
        canvas.restore();
    }

    private void drawMoon(Canvas canvas, float alpha) {
        float centerX = getWidth() * 0.8f;
        float centerY = getHeight() * 0.2f;
        glowPaint.setShader(moonGlow);
        glowPaint.setAlpha((int) (255.0f * alpha));
        canvas.save();
        canvas.translate(centerX, centerY);
        float glowScale = (moonRadius * 5.0f) / 100.0f;
        canvas.scale(glowScale, glowScale);
        canvas.drawCircle(0.0f, 0.0f, 100.0f, glowPaint);
        canvas.restore();
        solid.setStyle(Paint.Style.FILL);
        solid.setColor(withAlpha(MOON_CORE_COLOR, (int) (255.0f * alpha)));
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.drawPath(moonPath, solid);
        canvas.restore();
    }

    private void drawStars(Canvas canvas, float alpha) {
        solid.setStyle(Paint.Style.FILL);
        int length = starX.length;
        for (int starIndex = 0; starIndex < length; starIndex++) {
            float phase = starPhase[starIndex];
            float twinkle = 0.55f + 0.45f
                    * (float) Math.sin(time * (phase * 0.3f + 0.8f) + phase * 6.28f);
            solid.setColor(withAlpha(
                    STAR_COLOR,
                    coerceIn((int) (220.0f * twinkle * alpha))
            ));
            canvas.drawCircle(
                    starX[starIndex], starY[starIndex], starRadius[starIndex], solid
            );
        }
    }

    private void drawShooting(Canvas canvas, float deltaSeconds, float alpha) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        if (shootProgress < 0.0f) {
            shootDelay -= deltaSeconds;
            if (shootDelay <= 0.0f) {
                shootProgress = 0.0f;
                shootingStarX = rf(0.3f, 0.9f) * viewWidth;
                shootingStarY = rf(0.02f, 0.3f) * viewHeight;
                return;
            }
            return;
        }
        shootProgress += deltaSeconds / 0.8f;
        if (shootProgress > 1.0f) {
            shootProgress = -1.0f;
            shootDelay = rf(6.0f, 14.0f);
            return;
        }
        float headX = shootingStarX - ((shootProgress * 0.3f) * viewWidth);
        float headY = shootingStarY + (shootProgress * 0.18f * viewHeight);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(dp * 2.0f);
        line.setColor(Color.argb((int) (230.0f * (1.0f - shootProgress) * alpha), 255, 255, 255));
        canvas.drawLine(
                headX + (0.08f * viewWidth), headY - (0.048f * viewHeight),
                headX, headY,
                line
        );
    }

    private void drawFog(Canvas canvas, float deltaSeconds, float alpha) {
        float viewWidth = getWidth();
        fogPaint.setAlpha((int) (255.0f * alpha));
        for (FogLayer fog : fogs) {
            fog.positionX += fog.speed * deltaSeconds;
            if (fog.positionX - fog.scaleX * 100.0f > viewWidth) {
                fog.positionX = -fog.scaleX * 100.0f;
            }
            canvas.save();
            canvas.translate(fog.positionX, fog.positionY);
            canvas.scale(fog.scaleX, fog.scaleY);
            canvas.drawCircle(0.0f, 0.0f, 100.0f, fogPaint);
            canvas.restore();
        }
    }

    private void drawRain(Canvas canvas, float deltaSeconds, float alpha, boolean drawRipples) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        for (int rainDropIndex = 0; rainDropIndex < rainCount; rainDropIndex++) {
            rainY[rainDropIndex] += rainSpeed[rainDropIndex] * deltaSeconds;
            rainX[rainDropIndex] -= rainSpeed[rainDropIndex] * 0.22f * deltaSeconds;
            if (rainY[rainDropIndex] - rainLength[rainDropIndex] > viewHeight) {
                rainY[rainDropIndex] = -rainLength[rainDropIndex];
                rainX[rainDropIndex] = rf(0.0f, viewWidth * 1.3f);
            }
            if (rainX[rainDropIndex] < -60.0f) {
                rainX[rainDropIndex] += 1.3f * viewWidth;
            }
            line.setStrokeWidth(rainWidth[rainDropIndex]);
            line.setColor(withAlpha(
                    RAIN_STREAK_COLOR,
                    (int) (rainAlpha[rainDropIndex] * alpha)
            ));
            canvas.drawLine(rainX[rainDropIndex] + rainLength[rainDropIndex] * 0.22f,
                    rainY[rainDropIndex] - rainLength[rainDropIndex],
                    rainX[rainDropIndex], rainY[rainDropIndex], line);
        }
        if (!drawRipples) {
            return;
        }
        rippleTimer -= deltaSeconds;
        if (rippleTimer <= 0.0f) {
            rippleTimer = rf(0.02f, 0.08f) / intensity;
            for (int rippleIndex = 0; rippleIndex < rippleAge.length; rippleIndex++) {
                if (rippleAge[rippleIndex] >= 1.0f) {
                    rippleAge[rippleIndex] = 0.0f;
                    rippleX[rippleIndex] = rf(0.0f, viewWidth);
                    rippleY[rippleIndex] = rf(0.86f, 0.98f) * viewHeight;
                    break;
                }
            }
        }
        line.setStrokeWidth(dp * 1.2f);
        for (int rippleIndex = 0; rippleIndex < rippleAge.length; rippleIndex++) {
            if (rippleAge[rippleIndex] < 1.0f) {
                rippleAge[rippleIndex] += deltaSeconds / 0.9f;
                float radius = (rippleAge[rippleIndex] * 34.0f + 6.0f) * dp;
                line.setColor(withAlpha(
                        RAIN_RIPPLE_COLOR,
                        (int) (90.0f * (1.0f - Math.min(1.0f,
                                rippleAge[rippleIndex])) * alpha)
                ));
                rect.set(rippleX[rippleIndex] - radius,
                        rippleY[rippleIndex] - radius * 0.28f,
                        rippleX[rippleIndex] + radius,
                        rippleY[rippleIndex] + radius * 0.28f);
                canvas.drawOval(rect, line);
            }
        }
    }

    private void drawSnow(Canvas canvas, float deltaSeconds, float alpha) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        solid.setStyle(Paint.Style.FILL);
        for (int snowflakeIndex = 0; snowflakeIndex < snowCount; snowflakeIndex++) {
            fogY[snowflakeIndex] += fogSpeed[snowflakeIndex] * deltaSeconds;
            fogX[snowflakeIndex] += (((float) Math.sin(time * 0.9f + fogPhase[snowflakeIndex])
                    * 18.0f) - 6.0f) * dp * deltaSeconds;
            if (fogY[snowflakeIndex] - fogRadius[snowflakeIndex] > viewHeight) {
                fogY[snowflakeIndex] = -fogRadius[snowflakeIndex];
                fogX[snowflakeIndex] = rf(0.0f, viewWidth);
            }
            if (fogX[snowflakeIndex] < -fogRadius[snowflakeIndex]) {
                fogX[snowflakeIndex] += viewWidth;
            }
            if (fogX[snowflakeIndex] > fogRadius[snowflakeIndex] + viewWidth) {
                fogX[snowflakeIndex] -= viewWidth;
            }
            solid.setColor(Color.argb((int) (fogAlpha[snowflakeIndex] * alpha), 255, 255, 255));
            canvas.drawCircle(fogX[snowflakeIndex], fogY[snowflakeIndex],
                    fogRadius[snowflakeIndex], solid);
            if (fogRadius[snowflakeIndex] > dp * 2.8f) {
                solid.setColor(Color.argb((int) (fogAlpha[snowflakeIndex] * 0.25f * alpha),
                        255, 255, 255));
                canvas.drawCircle(fogX[snowflakeIndex], fogY[snowflakeIndex],
                        fogRadius[snowflakeIndex] * 1.8f, solid);
            }
        }
    }

    private void triggerBolt() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        ArrayList<Float> boltPoints = new ArrayList<>();
        float boltX = rf(0.15f, 0.85f) * viewWidth;
        float boltY = 0.0f;
        float boltEndY = rf(0.55f, 0.85f) * viewHeight;
        boltPoints.add(boltX);
        boltPoints.add(0.0f);
        float segmentHeight = viewHeight / 9.0f;
        while (boltY < boltEndY) {
            boltY += rf(0.6f, 1.2f) * segmentHeight;
            boltX += rf(-0.06f, 0.06f) * viewWidth;
            boltPoints.add(boltX);
            boltPoints.add(boltY);
        }
        lightningBolt = toFloatArray(boltPoints);
        flashProgress = 0.0f;
    }

    private void drawLightning(Canvas canvas, float deltaSeconds, float alpha, boolean showBolt) {
        float intensity = 0.8f;
        if (flashProgress >= 0.0f) {
            flashProgress += deltaSeconds;
            if (flashProgress > 0.8f) {
                flashProgress = -1.0f;
            }
        } else {
            flashDelay -= deltaSeconds;
            if (flashDelay <= 0.0f) {
                triggerBolt();
                flashDelay = rf(3.0f, 9.0f);
            }
        }
        float flashTime = flashProgress;
        if (flashTime < 0.0f) {
            return;
        }
        if (flashTime < 0.1f) {
            intensity = 1.0f;
        } else if (flashTime < 0.18f) {
            intensity = 0.15f;
        } else if (flashTime >= 0.3f) {
            intensity = Math.max(0.0f, (1.0f - ((flashTime - 0.3f) / 0.5f)) * 0.8f);
        }
        solid.setStyle(Paint.Style.FILL);
        solid.setColor(withAlpha(
                LIGHTNING_FLASH_COLOR,
                (int) (90.0f * intensity * alpha)
        ));
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), solid);
        if (showBolt && flashTime < 0.32f && lightningBolt.length >= 4) {
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeWidth(dp * 9.0f);
            line.setColor(withAlpha(
                    LIGHTNING_OUTER_BOLT_COLOR,
                    (int) (90.0f * intensity * alpha)
            ));
            for (int boltSegmentIndex = 0;
                 boltSegmentIndex + 3 < lightningBolt.length;
                 boltSegmentIndex += 2) {
                canvas.drawLine(
                        lightningBolt[boltSegmentIndex], lightningBolt[boltSegmentIndex + 1],
                        lightningBolt[boltSegmentIndex + 2], lightningBolt[boltSegmentIndex + 3],
                        line
                );
            }
            line.setStrokeWidth(dp * 2.6f);
            line.setColor(withAlpha(
                    LIGHTNING_CORE_BOLT_COLOR,
                    (int) (255.0f * intensity * alpha)
            ));
            for (int boltPointIndex = 0; boltPointIndex + 3 < lightningBolt.length; boltPointIndex += 2) {
                canvas.drawLine(
                        lightningBolt[boltPointIndex], lightningBolt[boltPointIndex + 1],
                        lightningBolt[boltPointIndex + 2], lightningBolt[boltPointIndex + 3],
                        line
                );
            }
        }
    }
}
