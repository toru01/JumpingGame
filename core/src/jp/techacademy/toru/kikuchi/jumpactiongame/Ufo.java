package jp.techacademy.toru.kikuchi.jumpactiongame;

import com.badlogic.gdx.graphics.Texture;

// 最後にゴールにプレイヤーが触れることでクリアとなるので、このUfoクラスのサイズを変更することで若干難易度が変わります。
// ゲームバランスを調整するときはUFO_WIDTHとUFO_HEIGHTの値を変更してみるのも良いでしょう。
public class Ufo extends GameObject {
    // 横幅、高さ
    public static final float UFO_WIDTH = 2.0f;
    public static final float UFO_HEIGHT = 1.3f;

    public Ufo(Texture texture, int srcX, int srcY, int srcWidth, int srcHeight) {
        super(texture, srcX, srcY, srcWidth, srcHeight);
        setSize(UFO_WIDTH, UFO_HEIGHT);
    }
}