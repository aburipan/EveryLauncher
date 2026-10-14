package jp.aburipan.everylauncher;

import android.app.Application;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class EveryLauncher extends Application {
    private static final int BACKGROUND_THREAD_COUNT = 2;

    private Executor backgroundExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_THREAD_COUNT);
    }

    public Executor backgroundExecutor() {
        return backgroundExecutor;
    }
}
