package jp.aburipan.everylauncher.weather;

final class CloudLayer {
    float positionX;
    float positionY;
    float scale;
    float speed;
    float width;
    float[] puffs;

    CloudLayer(float positionX, float positionY, float scale, float speed, float width, float[] puffs) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.scale = scale;
        this.speed = speed;
        this.width = width;
        this.puffs = puffs;
    }
}
