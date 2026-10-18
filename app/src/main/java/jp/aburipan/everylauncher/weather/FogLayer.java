package jp.aburipan.everylauncher.weather;

final class FogLayer {
    float positionX;
    float positionY;
    float scaleX;
    float scaleY;
    float speed;

    FogLayer(float positionX, float positionY, float scaleX, float scaleY, float speed) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.speed = speed;
    }
}
