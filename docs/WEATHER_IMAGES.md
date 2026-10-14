# 天気背景画像

天気背景に使う画像は任意です。  
ファイル名は次のいずれかにし、拡張子は`jpg`、`jpeg`、`png`、`webp` を使用してください。

| ファイル名 | 使用する天気 | 検索キーワード例 |
|---|---|---|
| `clear_day` | 快晴（昼） | blue sky sun / sunny sky |
| `clear_night` | 快晴（夜） | starry night sky / milky way |
| `partly` | 薄曇り | blue sky white clouds |
| `cloudy` | くもり | overcast sky / grey clouds |
| `fog` | 霧 | foggy forest / misty landscape |
| `rain` | 雨 | rain street / rainy window |
| `snow` | 雪 | snowfall / winter snow forest |
| `storm` | 雷雨 | thunderstorm lightning / dark storm clouds |

夜の `partly`、`cloudy`、`fog`、`rain`、`snow`、`storm` は、対応する画像を暗くして使います。  
画像がない場合は、天気に応じた内蔵背景を表示します。

## 画像をアプリに同梱

画像を`app/src/main/assets/weather/`に置いてアプリをビルドします。

例:
```text
app/src/main/assets/weather/clear_day.jpg
app/src/main/assets/weather/rain.webp
```

## ADBで端末に追加

再ビルドせずに追加するには、アプリの外部ファイル領域へコピーします。  
外部ファイルの画像は同梱画像より優先されます。

```sh
adb shell mkdir -p /sdcard/Android/data/jp.aburipan.everylauncher/files/weather
adb push clear_day.jpg /sdcard/Android/data/jp.aburipan.everylauncher/files/weather/
```

他の画像も同じディレクトリへコピーしてください。画面を開き直すと画像が再確認されます。

横長の画像を推奨します。長辺は2000〜2560px程度、ファイルサイズは1枚あたり1MB前後が目安です。
