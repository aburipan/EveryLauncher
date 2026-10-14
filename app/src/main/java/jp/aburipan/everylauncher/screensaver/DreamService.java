package jp.aburipan.everylauncher.screensaver;

import android.content.Intent;

import jp.aburipan.everylauncher.LauncherActivity;

public final class DreamService extends android.service.dreams.DreamService {
    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        startActivity(new Intent(this, LauncherActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
