# Every Launcher

**Every Launcher** は、Android 9（API 28）向けの、スマートディスプレイ風のホームランチャーです。時計・天気・予定・タイマーを、横スワイプで切り替えて使えます。

> **English summary:** Every Launcher is a smart-display–style home launcher for Android 9 (landscape). Swipe between a 24-hour weather band, a clock/weather home screen, a 7-day calendar and stopwatch/timer tools. Weather comes from [Open-Meteo](https://open-meteo.com/) (no API key), events from the device calendar. Written in Kotlin with plain Views, no third-party UI libraries except ViewPager2.

> このアプリは非公式で、Google LLC とは関係ありません。Google の Nest Hub などのスマートディスプレイにインスパイアされていますが、「Nest」「Nest Hub」は Google LLC の商標です。

<!--
## スクリーンショット
docs/screenshots/ に画像を置いて、ここに貼ってください。
![home](docs/screenshots/home.png)
-->

## 特長

画面は横スワイプで 4 ページを行き来します（起動時はホーム）。

| ページ | 内容 |
|---|---|
| 1時間ごとの天気（左端） | 今から24時間分を1時間ごとに横スクロールで表示（1画面に6個） |
| **ホーム** | 大きな時計と日付、現在の天気カード、次の予定カード、アプリ一覧ボタン |
| 今後7日間の予定 | 端末のカレンダーから7日分の予定を表示 |
| ストップウォッチ・タイマー | ストップウォッチ、タイマー、アラームアプリへのショートカット |

### 操作

- **時計をタップ** → 秒まで表示する大きな時計
- **天気カードをタップ** → 7日間の天気予報
- **次の予定カードをタップ** → 予定ページへ移動
- **ストップウォッチ／タイマーのカードをタップ** → 全画面表示
- 拡大表示は、タップしたカードから広がる短いアニメーションで開き、「閉じる」ボタンか戻る操作で閉じます
- 1時間ごとの天気では、帯の上のドラッグで帯がスクロールし、帯の外の空きスペースのドラッグでページが切り替わります

### タイマー

- プリセット（1・3・5・10分）と、時・分・秒を回して選ぶ自由設定
- 開始 / 一時停止 / 再開 / 停止
- 終了時はアラーム音量でアラーム音が鳴ります（最長1分。停止か開始で止まります）。アラーム音量が0の場合は半分まで上げます

### 予定

- 端末のカレンダー（Google カレンダーを同期していればその予定）を **30秒ごと** に読み直します
- 終わった予定は一覧から消えます（終日の予定は、その日が終わると消えます）
- 最大20件まで表示します

### 天気と背景

- 天気データは [Open-Meteo](https://open-meteo.com/) から取得します（APIキー不要）。現在の天気は15分ごとに更新します
- 背景は天気（快晴・薄曇り・くもり・霧・雨・雪・雷雨）と昼夜で変わり、全ページで同じ映像を共有します
  - 写真を用意した天気は、写真をゆっくり拡大・移動させ、雨・雪・稲光などを重ねます
  - 写真がない天気は、内蔵の描画アニメーション（空、雲、星と月、霧、雨、雪、稲光）で表示します
- 写真の置き方は [WEATHER_IMAGES.md](WEATHER_IMAGES.md) を参照してください。**写真はこのリポジトリに含まれていません**

## 動作環境

- Android 9（API 28）の端末。横向き固定のフルスクリーンです
- 天気の取得にインターネット接続が必要です
- 予定の表示には、端末にカレンダーが同期されている必要があります

## 権限とプライバシー

| 権限 | 用途 |
|---|---|
| `INTERNET` | 天気予報の取得 |
| `ACCESS_COARSE_LOCATION` | 現在地の天気を取るためのおおよその位置情報 |
| `READ_CALENDAR` | 予定の表示 |

- 位置情報（緯度・経度）は、天気を取得するために Open-Meteo に送られます。それ以外のサーバーへの通信はありません
- カレンダーの内容は端末内で読み取るだけで、外部には送信しません
- 広告・解析・ログイン機能はありません

## インストール

### 1. APKを用意する

**A. GitHub Actions でビルドする（Android Studio 不要）**

1. このリポジトリをフォーク（または自分のリポジトリにコピー）します
2. **Actions** タブで **Build APK** を実行します（`workflow_dispatch` で手動実行できます）
3. 完了した実行の **Artifacts** から `app-debug` をダウンロードし、解凍して `app-debug.apk` を取り出します

**B. Android Studio でビルドする**

1. このリポジトリを開き、Gradle Sync を実行します
2. **Build > Build APK(s)** を実行します（出力: `app/build/outputs/apk/debug/app-debug.apk`）

### 2. adb でインストールしてホームに設定する

```sh
adb install -r app-debug.apk
adb shell pm grant com.example.everylauncher android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.everylauncher android.permission.READ_CALENDAR
adb shell cmd package set-home-activity com.example.everylauncher/.MainActivity
```

`set-home-activity` が使えない端末では、ホームボタンを押して表示される選択画面で「常時」を選んでください。

元のホームに戻すには、アンインストールします。

```sh
adb uninstall com.example.everylauncher
```

> デバッグ署名の APK です。Google Play での配布には、署名の設定と `applicationId`（現在は `com.example.everylauncher`）を自分のものに変更する必要があります。

## ビルド環境

| 項目 | バージョン |
|---|---|
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| Kotlin | 1.9.24 |
| JDK | 17 |
| compileSdk / minSdk / targetSdk | 34 / 28 / 28 |

## プロジェクト構成

```
EveryLauncher/
├─ .github/workflows/      # APKを自動ビルドするワークフロー
├─ app/src/main/
│  ├─ AndroidManifest.xml  # HOME ランチャーとして登録
│  ├─ assets/weather/      # 天気の写真を同梱する場合の置き場所（任意）
│  └─ java/com/example/everylauncher/
│     └─ MainActivity.kt   # 画面・データ取得・WeatherView（背景アニメーション）
├─ WEATHER_IMAGES.md       # 天気写真のファイル名と置き方
└─ README.md
```

レイアウトのXMLは使わず、画面はすべてコードで組み立てています。外部ライブラリは `androidx.viewpager2` のみです。

## カスタマイズ

主な値は `MainActivity.kt` にまとまっています。

| 内容 | 場所 |
|---|---|
| 予定の更新間隔（30秒） | `ticker` の `postDelayed` と `loadCalendar()` |
| 天気の更新間隔（15分） | `ticker` 内の `tick % 30` |
| 1時間ごとの天気の表示個数・余白・位置 | `hourlyPage()` / `buildHourly()` / `hourScale()` |
| 背景の描画 | クラス `WeatherView` |

## 既知の制限

- 縦向きには対応していません（横向き固定）
- 位置情報は、端末が持っている最後の位置か、1回だけの取得を使います。位置情報がオフの端末では天気が表示されません
- 天気アイコンは絵文字なので、端末のフォントによって見た目が変わります
- 背景のアニメーションは常時動くため、バッテリーを多めに消費します

## クレジット

- 天気データ: [Open-Meteo.com](https://open-meteo.com/)（[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)）。無料APIは非商用利用向けです。商用で使う場合は Open-Meteo の利用条件を確認してください

## ライセンス

[MIT License](LICENSE)
Created by Claude
