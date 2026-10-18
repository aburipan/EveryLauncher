package jp.aburipan.everylauncher.weather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jp.aburipan.everylauncher.EveryLauncher;
import jp.aburipan.everylauncher.LauncherActivity;
import jp.aburipan.everylauncher.R;

@SuppressWarnings({"NullableProblems", "deprecation"})
public final class WeatherLoader {
    private static final int FIRST_ENABLED_PROVIDER_INDEX = 0;
    private final LauncherActivity activity;
    private final Consumer<Forecast> onForecast;
    private final Consumer<String> onError;
    private final Executor backgroundExecutor;
    private boolean loading;

    public WeatherLoader(LauncherActivity activity, Consumer<Forecast> onForecast, Consumer<String> onError) {
        this.activity = activity;
        this.onForecast = onForecast;
        this.onError = onError;
        backgroundExecutor = ((EveryLauncher) activity.getApplication())
                .backgroundExecutor();
    }

    public void load() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onError.accept(activity.getString(R.string.weather_permission_required));
            return;
        }
        LocationManager locationManager = (LocationManager) activity.getSystemService(
                android.content.Context.LOCATION_SERVICE);
        List<String> enabledProviders = locationManager.getProviders(true);
        List<Location> knownLocations = new ArrayList<>();
        for (String provider : enabledProviders) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
            if (lastKnownLocation != null) {
                knownLocations.add(lastKnownLocation);
            }
        }
        Location latestLocation = null;
        for (Location candidateLocation : knownLocations) {
            if (latestLocation == null || candidateLocation.getTime() > latestLocation.getTime()) latestLocation = candidateLocation;
        }
        if (latestLocation != null) {
            fetchWeather(latestLocation.getLatitude(), latestLocation.getLongitude());
        } else if (enabledProviders.isEmpty()) {
            onError.accept(activity.getString(R.string.location_disabled));
        } else {
            onError.accept(activity.getString(R.string.location_loading));
            locationManager.requestSingleUpdate(
                    enabledProviders.get(FIRST_ENABLED_PROVIDER_INDEX), new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (isActivityActive()) {
                        fetchWeather(location.getLatitude(), location.getLongitude());
                    }
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
            }, Looper.getMainLooper());
        }
    }

    private void fetchWeather(double latitude, double longitude) {
        if (loading || !isActivityActive()) return;
        loading = true;
        backgroundExecutor.execute(() -> {
            try {
                Forecast forecast = WeatherRepository.load(activity, latitude, longitude);
                activity.runOnUiThread(() -> {
                    loading = false;
                    if (isActivityActive()) onForecast.accept(forecast);
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> {
                    loading = false;
                    if (isActivityActive()) {
                        onError.accept(activity.getString(R.string.weather_unavailable));
                    }
                });
            }
        });
    }

    private boolean isActivityActive() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }
}
