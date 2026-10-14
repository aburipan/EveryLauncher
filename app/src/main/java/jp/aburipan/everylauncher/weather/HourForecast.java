package jp.aburipan.everylauncher.weather;

public final class HourForecast {
    public final int hour;
    public final int temperature;
    public final int weatherCode;
    public final int precipitationChance;
    public final boolean isDay;
    public final String dateLabel;

    HourForecast(int hour, int temperature, int weatherCode, int precipitationChance,
                 boolean isDay, String dateLabel) {
        this.hour = hour;
        this.temperature = temperature;
        this.weatherCode = weatherCode;
        this.precipitationChance = precipitationChance;
        this.isDay = isDay;
        this.dateLabel = dateLabel;
    }
}
