# インストールとビルド

## Android Studioでビルド

1. Android Studio で EveryLauncher のプロジェクトを開きます。
2. Gradle Syncが完了したら、**Build > Build APK(s)** を実行します。
3. APKは`app/build/outputs/apk/debug/app-debug.apk`に生成されます。

ローカルビルドには JDK 21 を使用してください。

## GitHub Actionsでビルド

リポジトリの **Actions** タブから **Build** ワークフローを実行します。  
完了後、Artifacts から APKをダウンロードできます。

## APKを端末にインストール

```sh
adb install -r -d -g app-debug.apk
adb shell am start -n jp.aburipan.everylauncher/.LauncherActivity
```

## ホームアプリとして設定

端末のホームアプリ選択画面で **Every Launcher** を選びます。端末によって設定画面の場所や名称が異なります。

ADBで設定する場合:

```sh
adb shell cmd package set-home-activity jp.aburipan.everylauncher/.LauncherActivity
```

このアプリは通常のランチャーアプリ一覧からも起動できます。

## スクリーンセーバーとして設定

端末の **設定** で、スクリーンセーバーの項目を開き、**Every Launcher** を選びます。  
スクリーンセーバーが開始すると、メイン画面が起動します。

```sh
adb shell settings put secure screensaver_components jp.aburipan.everylauncher/jp.aburipan.everylauncher.screensaver.DreamService
```

## 元のホームに戻す

端末のホームアプリ設定から別のホームアプリを選ぶか、Every Launcherをアンインストールします。

```sh
adb uninstall jp.aburipan.everylauncher
```