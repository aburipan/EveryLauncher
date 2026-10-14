package jp.aburipan.everylauncher.weather;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import jp.aburipan.everylauncher.AppConstants;
import jp.aburipan.everylauncher.R;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WeatherRepository {
    private static final String API_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String LATITUDE_PARAMETER = "?latitude=";
    private static final String LONGITUDE_PARAMETER = "&longitude=";
    private static final String CURRENT_FIELDS = "&current=temperature_2m,weather_code,is_day";
    private static final String HOURLY_FIELDS = "&hourly=temperature_2m,weather_code,precipitation_probability,is_day";
    private static final String DAILY_FIELDS = "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max";
    private static final String TIMEZONE_PARAMETER = "&timezone=auto";
    private static final String FORECAST_DAYS_PARAMETER = "&forecast_days=";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String JSON_CURRENT = "current";
    private static final String JSON_DAILY = "daily";
    private static final String JSON_HOURLY = "hourly";
    private static final String JSON_TIME = "time";
    private static final String JSON_TEMPERATURE = "temperature_2m";
    private static final String JSON_HIGH_TEMPERATURE = "temperature_2m_max";
    private static final String JSON_LOW_TEMPERATURE = "temperature_2m_min";
    private static final String JSON_WEATHER_CODE = "weather_code";
    private static final String JSON_IS_DAY = "is_day";
    private static final String JSON_PRECIPITATION = "precipitation_probability";
    private static final String JSON_MAX_PRECIPITATION = "precipitation_probability_max";
    private static final int CURRENT_HOUR_PREFIX_LENGTH = 13;
    private static final int CURRENT_TIME_START_INDEX = 0;
    private static final int NO_FORECAST_INDEX = 0;
    private static final int TODAY_FORECAST_INDEX = 0;
    private static final int TOMORROW_FORECAST_INDEX = 1;
    private static final int MIDNIGHT_HOUR = 0;
    private static final int HOUR_START_INDEX = 11;
    private static final int HOUR_END_INDEX = 13;
    private static final int DATE_START_INDEX = 0;
    private static final int DATE_END_INDEX = 10;
    private static final int HOURLY_FORECAST_LIMIT = 24;
    private static final int FORECAST_DAYS = 7;
    private static final int DEFAULT_PRECIPITATION = 0;
    private static final int DAYLIGHT_VALUE = 1;
    private static final int DEFAULT_WEATHER_CODE = 0;
    private static final double DEFAULT_TEMPERATURE = 0.0d;
    private static final int READER_BUFFER_SIZE = 8192;
    private static final int TEXT_BUFFER_SIZE = 4096;
    private static final int CONNECTION_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    public static Forecast load(Context context, double latitude, double longitude) throws Exception {
        String requestUrl = API_BASE_URL
                + LATITUDE_PARAMETER + latitude
                + LONGITUDE_PARAMETER + longitude
                + CURRENT_FIELDS
                + HOURLY_FIELDS
                + DAILY_FIELDS
                + TIMEZONE_PARAMETER
                + FORECAST_DAYS_PARAMETER + FORECAST_DAYS;

        HttpsURLConnection connection = (HttpsURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        JSONObject response;
        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8), READER_BUFFER_SIZE)) {
            response = new JSONObject(readText(reader));
        } finally {
            connection.disconnect();
        }

        JSONObject current = response.getJSONObject(JSON_CURRENT);
        JSONObject daily = response.getJSONObject(JSON_DAILY);
        JSONObject hourly = response.getJSONObject(JSON_HOURLY);
        long temperature = Math.round(current.getDouble(JSON_TEMPERATURE));
        long highTemperature = Math.round(
                daily.getJSONArray(JSON_HIGH_TEMPERATURE).getDouble(0)
        );
        long lowTemperature = Math.round(
                daily.getJSONArray(JSON_LOW_TEMPERATURE).getDouble(0)
        );
        int weatherCode = current.getInt(JSON_WEATHER_CODE);
        boolean isDay = current.optInt(JSON_IS_DAY, DAYLIGHT_VALUE) == DAYLIGHT_VALUE;

        List<HourForecast> hourlyForecast = parseHourlyForecast(context, current, hourly);
        List<DayForecast> dailyForecast = parseDailyForecast(context, daily);
        return new Forecast(temperature, highTemperature, lowTemperature, weatherCode,
                isDay, hourlyForecast, dailyForecast);
    }

    private static List<HourForecast> parseHourlyForecast(Context context, JSONObject current, JSONObject hourly)
            throws Exception {
        JSONArray times = hourly.getJSONArray(JSON_TIME);
        JSONArray temperatures = hourly.getJSONArray(JSON_TEMPERATURE);
        JSONArray weatherCodes = hourly.getJSONArray(JSON_WEATHER_CODE);
        JSONArray precipitation = hourly.getJSONArray(JSON_PRECIPITATION);
        JSONArray daylight = hourly.getJSONArray(JSON_IS_DAY);
        String currentHour = current.getString(JSON_TIME).substring(
                CURRENT_TIME_START_INDEX,
                CURRENT_HOUR_PREFIX_LENGTH
        );

        int firstForecastIndex = NO_FORECAST_INDEX;
        for (int index = 0; index < times.length(); index++) {
            if (times.getString(index).startsWith(currentHour)) {
                firstForecastIndex = index;
                break;
            }
        }

        SimpleDateFormat dateParser = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(context.getString(R.string.forecast_date_format), Locale.getDefault());
        List<HourForecast> forecasts = new ArrayList<>();
        for (int index = firstForecastIndex;
             index < times.length() && forecasts.size() < HOURLY_FORECAST_LIMIT;
             index++) {
            String time = times.getString(index);
            int hour = Integer.parseInt(time.substring(HOUR_START_INDEX, HOUR_END_INDEX));
            String dateLabel = AppConstants.EMPTY_STRING;
            if (forecasts.isEmpty() || hour == MIDNIGHT_HOUR) {
                Date date = Objects.requireNonNull(
                        dateParser.parse(time.substring(DATE_START_INDEX, DATE_END_INDEX))
                );
                dateLabel = dateFormatter.format(date);
            }
            forecasts.add(new HourForecast(
                    hour,
                    (int) Math.round(temperatures.optDouble(index, DEFAULT_TEMPERATURE)),
                    weatherCodes.optInt(index, DEFAULT_WEATHER_CODE),
                    precipitation.optInt(index, DEFAULT_PRECIPITATION),
                    daylight.optInt(index, DAYLIGHT_VALUE) == DAYLIGHT_VALUE,
                    dateLabel));
        }
        return forecasts;
    }

    private static List<DayForecast> parseDailyForecast(Context context, JSONObject daily) throws Exception {
        JSONArray times = daily.getJSONArray(JSON_TIME);
        JSONArray weatherCodes = daily.getJSONArray(JSON_WEATHER_CODE);
        JSONArray highs = daily.getJSONArray(JSON_HIGH_TEMPERATURE);
        JSONArray lows = daily.getJSONArray(JSON_LOW_TEMPERATURE);
        JSONArray precipitation = daily.getJSONArray(JSON_MAX_PRECIPITATION);
        SimpleDateFormat dateParser = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(context.getString(R.string.forecast_date_format), Locale.getDefault());
        List<DayForecast> forecasts = new ArrayList<>();

        for (int index = 0; index < times.length(); index++) {
            String label;
            if (index == TODAY_FORECAST_INDEX) {
                label = context.getString(R.string.today);
            } else if (index == TOMORROW_FORECAST_INDEX) {
                label = context.getString(R.string.tomorrow);
            } else {
                Date date = Objects.requireNonNull(dateParser.parse(times.getString(index)));
                label = dateFormatter.format(date);
            }
            forecasts.add(new DayForecast(
                    label,
                    weatherCodes.optInt(index, DEFAULT_WEATHER_CODE),
                    (int) Math.round(highs.optDouble(index, DEFAULT_TEMPERATURE)),
                    (int) Math.round(lows.optDouble(index, DEFAULT_TEMPERATURE)),
                    precipitation.optInt(index, DEFAULT_PRECIPITATION)));
        }
        return forecasts;
    }

    private static String readText(BufferedReader reader) throws java.io.IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[TEXT_BUFFER_SIZE];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            text.append(buffer, 0, count);
        }
        return text.toString();
    }

}
