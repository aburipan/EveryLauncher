package jp.aburipan.everylauncher.weather;

import java.util.List;

public final class Forecast {
    public final long temperature;
    public final long highTemperature;
    public final long lowTemperature;
    public final int weatherCode;
    public final boolean isDay;
    public final List<HourForecast> hourly;
    public final List<DayForecast> daily;

    Forecast(long temperature, long highTemperature, long lowTemperature, int weatherCode,
             boolean isDay, List<HourForecast> hourly, List<DayForecast> daily) {
        this.temperature = temperature;
        this.highTemperature = highTemperature;
        this.lowTemperature = lowTemperature;
        this.weatherCode = weatherCode;
        this.isDay = isDay;
        this.hourly = hourly;
        this.daily = daily;
    }
}
