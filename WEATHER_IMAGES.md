# 天気背景の写真

ファイル名（拡張子は jpg / jpeg / png / webp）:

| ファイル名 | 使う天気 | 探すキーワード例 |
|---|---|---|
| clear_day | 快晴（昼） | blue sky sun / sunny sky |
| clear_night | 快晴（夜） | starry night sky / milky way |
| partly | 薄曇り | blue sky white clouds |
| cloudy | くもり | overcast sky / grey clouds |
| fog | 霧 | foggy forest / misty landscape |
| rain | 雨 | rain street / rainy window |
| snow | 雪 | snowfall / winter snow forest |
| storm | 雷雨 | thunderstorm lightning / dark storm clouds |

夜の partly / cloudy / fog / rain / snow / storm は、同じ写真を自動で暗くして使います。
写真が無い天気は、これまでの描画アニメーションで表示します。

## 入れ方
A) アプリに同梱（再ビルドが必要）
   app/src/main/assets/weather/ に上のファイル名で置いて GitHub にアップロード。

B) ビルド不要（adb push）
   adb shell mkdir -p /sdcard/Android/data/com.example.everylauncher/files/weather
   adb push clear_day.jpg /sdcard/Android/data/com.example.everylauncher/files/weather/
   （他も同様。ランチャーを開き直すと反映）
   こちらが同梱より優先されます。

推奨: 横長、長辺 2000〜2560px、1枚 1MB 前後。
