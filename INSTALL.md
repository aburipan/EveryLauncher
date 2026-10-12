# Every Launcher (Android 9 / API 28)

## ビルド
Android Studio で EveryLauncher フォルダを開き、Sync → Build > Build APK(s)
（出力: app/build/outputs/apk/debug/app-debug.apk）

## adb でインストール
adb devices
adb install -r app-debug.apk
adb shell cmd package set-home-activity com.example.everylauncher/.MainActivity
adb shell pm grant com.example.everylauncher android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.everylauncher android.permission.READ_CALENDAR
adb shell am start -n com.example.everylauncher/.MainActivity

## 元のホームに戻す
adb shell pm clear com.example.everylauncher   # もしくはアンインストール
adb uninstall com.example.everylauncher
