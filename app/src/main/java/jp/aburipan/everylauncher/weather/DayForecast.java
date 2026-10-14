package jp.aburipan.everylauncher.weather;

public final class DayForecast {
    public final String dateLabel;
    public final int weatherCode;
    public final int highTemperature;
    public final int lowTemperature;
    public final int precipitationChance;

    DayForecast(String dateLabel, int weatherCode, int highTemperature,
                int lowTemperature, int precipitationChance) {
        this.dateLabel = dateLabel;
        this.weatherCode = weatherCode;
        this.highTemperature = highTemperature;
        this.lowTemperature = lowTemperature;
        this.precipitationChance = precipitationChance;
    }
}
