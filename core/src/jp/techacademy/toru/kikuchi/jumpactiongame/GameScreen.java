package jp.techacademy.toru.kikuchi.jumpactiongame;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
    Todo: 本レッスンで制作した JumpActionGame プロジェクトを基に制作してください -Done
    Todo: ゲーム中に敵キャラ(Enemyクラス)を出現させてください
    Todo: 敵キャラに衝突したときに、ゲームを終了させてください
    Todo: また、敵キャラと衝突した際に衝突音を出してください（音はフリーのものなどを探してみましょう）-Done
 */

// 各画面に相当するScreenはScreenAdapterクラスを継承します。コンストラクタでは引数で受け取ったJumpActionGameクラスのオブジェクトをメンバ変数に保持します。
public class GameScreen extends ScreenAdapter {
    // カメラの定数
    static final float CAMERA_WIDTH = 10;
    static final float CAMERA_HEIGHT = 15;
    // それでは詳細を見ていきましょう。まず定数からです。カメラはゲーム世界の一部を切り取って表示するものでしたが、ゲーム世界の広さはまだ定義していませんでした。
    // JumpActionGameは上に登っていくゲームなので横幅はカメラと同じ10、高さは20画面分設定します。
    // この20を修正すればゲーム世界の高さが変わります。開発中はもう少し小さい値に設定してゴールの動作確認を行い易くするというのも1つの手でしょう。
    static final float WORLD_WIDTH = 10;
    static final float WORLD_HEIGHT = 15 * 20; // 20画面分登れば終了
    static final float GUI_WIDTH = 320;
    static final float GUI_HEIGHT = 480;

    // GAME_STATE_READY：ゲーム開始前
    // GAME_STATE_PLAYING：ゲーム中
    // GAME_STATE_GAMEOVER：ゴールか落下してゲーム終了
    static final int GAME_STATE_READY = 0;
    static final int GAME_STATE_PLAYING = 1;
    static final int GAME_STATE_GAMEOVER = 2;

    // 重力
    static final float GRAVITY = -18;

    private Sound sound;
    private Sound bgm_sound;
    private long bgmid;

    private JumpActionGame mGame;

    // スプライトとはコンピュータの処理の負荷を上げずに高速に画像を描画する仕組みです。「プレイヤーや地面などの画像を表示するためのもの」という認識で問題ありません。
    Sprite mBg;
    OrthographicCamera mCamera;
    OrthographicCamera mGuiCamera;

    FitViewport mViewPort;
    FitViewport mGuiViewPort;

    /*Random mRandom：乱数（ランダムに生成される数字）を取得するためのクラス
    List mSteps：生成して配置した踏み台を保持するリスト
    List mStars：生成して配置した★を保持するリスト
    Ufo mUfo：生成して配置したUFO（ゴール）を保持する
    Player mPlayer：生成して配置したプレイヤーを保持する
    int mGameState：ゲームの状態を保持する
    */
    Random mRandom;
    List<Step> mSteps;
    List<Star> mStars;
    List<Enemy> mEnemy;
    Ufo mUfo;
    Player mPlayer;

    float mHeightSoFar;
    int mGameState;
    Vector3 mTouchPoint;
    BitmapFont mFont;
    int mScore;
    int mHighScore;
    Preferences mPrefs;


    //起動時に読み込まれるクラス
    public GameScreen(JumpActionGame game) {
        mGame = game;

        // Textureクラスはテクスチャを表すクラスで、スプライトに貼り付ける画像のことです。Textureクラスは画像のファイル名を指定して生成します。
        // 背景の準備
        Texture bgTexture = new Texture("back.png");
        // SpriteクラスにTextureRegionクラスのオブジェクトとして生成したものを指定します。
        // TextureRegionで切り出す時の原点は左上
        mBg = new Sprite(new TextureRegion(bgTexture, 0, 0, 540, 810));
        mBg.setSize(CAMERA_WIDTH, CAMERA_HEIGHT);
        mBg.setPosition(0, 0);

        // カメラ、ViewPortを生成、設定する
        mCamera = new OrthographicCamera();
        mCamera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT);
        mViewPort = new FitViewport(CAMERA_WIDTH, CAMERA_HEIGHT, mCamera);

        // GUI用のカメラを設定する
        mGuiCamera = new OrthographicCamera();
        mGuiCamera.setToOrtho(false, GUI_WIDTH, GUI_HEIGHT);
        mGuiViewPort = new FitViewport(GUI_WIDTH, GUI_HEIGHT, mGuiCamera);

        // メンバ変数の初期化
        mRandom = new Random();
        mSteps = new ArrayList<Step>();
        mStars = new ArrayList<Star>();
        mEnemy = new ArrayList<Enemy>();
        mGameState = GAME_STATE_READY;
        mTouchPoint = new Vector3();
        mFont = new BitmapFont(Gdx.files.internal("font.fnt"), Gdx.files.internal("font.png"), false);
        mFont.getData().setScale(0.8f);
        mScore = 0;

        // ハイスコアをPreferencesから取得する
        mPrefs = Gdx.app.getPreferences("jp.techacademy.toru.kikuchi.jumpactiongame"); // ←追加する
        mHighScore = mPrefs.getInteger("HIGHSCORE", 0); // ←追加する

        // 音の読み込み
        sound = Gdx.audio.newSound(Gdx.files.internal("data/sound_effect.mp3"));
        bgm_sound = Gdx.audio.newSound(Gdx.files.internal("data/sound_bgm.mp3"));
        createStage();
    }

    //1/60秒フレームの処理
    @Override
    public void render(float delta) {
        // 状態を更新する
        update(delta);

        // glClearColorメソッドは画面をクリアする時の色を赤、緑、青、透過で指定します。
        // そしてGdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);で実際にその色でクリア（塗りつぶし）を行います。
        // 描画する
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // カメラの中心を超えたらカメラを上に移動させる つまりキャラが画面の上半分には絶対に行かない
        if (mPlayer.getY() > mCamera.position.y) {
            mCamera.position.y = mPlayer.getY();
        }

        // カメラの座標をアップデート（計算）し、スプライトの表示に反映させる
        // カメラのupdateメソッドでは行列演算を行ってカメラの座標値の再計算を行ってくれるメソッドです。
        // そしてsetProjectionMatrixメソッドとcombinedメソッドでその座標をスプライトに反映しています。
        mCamera.update();
        // SpriteBatchクラスのsetProjectionMatrixメソッドをOrthographicCameraクラスのcombinedプロパティを引数に与えて呼び出します。
        // これはカメラの座標をアップデート（計算）し、スプライトの表示に反映させるために必要な呼び出しです。
        // これらの呼び出しによって物理ディスプレイに依存しない表示を行うことができます。
        mGame.batch.setProjectionMatrix(mCamera.combined);

        mGame.batch.begin();

        // 背景
        // 原点は左下
        mBg.setPosition(mCamera.position.x - CAMERA_WIDTH / 2, mCamera.position.y - CAMERA_HEIGHT / 2);
        mBg.draw(mGame.batch);

        // Step
        for (int i = 0; i < mSteps.size(); i++) {
            mSteps.get(i).draw(mGame.batch);
        }

        // Star
        for (int i = 0; i < mStars.size(); i++) {
            mStars.get(i).draw(mGame.batch);
        }

        // Enemy
        for (int i = 0; i < mEnemy.size(); i++) {
            mEnemy.get(i).draw(mGame.batch);
        }

        // UFO
        mUfo.draw(mGame.batch);

        //Player
        mPlayer.draw(mGame.batch);

        mGame.batch.end();

        // スコア表示
        mGuiCamera.update(); // ←追加する
        mGame.batch.setProjectionMatrix(mGuiCamera.combined); // ←追加する
        mGame.batch.begin(); // ←追加する
        mFont.draw(mGame.batch, "HighScore: " + mHighScore, 16, GUI_HEIGHT - 15); // ←追加する
        mFont.draw(mGame.batch, "Score: " + mScore, 16, GUI_HEIGHT - 35); // ←追加する
        mGame.batch.end(); // ←追加する

    }

    // 最後にresizeメソッドをオーバーライドしてFitViewportクラスのupdateメソッドを呼び出します。
    // resizeメソッドは物理的な画面のサイズが変更されたときに呼び出されるメソッドです。Androidではcreate直後やバックグランドから復帰したときに呼び出されます。
    @Override
    public void resize(int width, int height) {
        mViewPort.update(width, height);
        mGuiViewPort.update(width, height);
    }


    // createStageメソッドはオブジェクトを配置するメソッドです。踏み台をプレイヤーがジャンプできる高さ以下の距離を空けつつ配置していきます。
    // 乱数はmRandom.nextFloat()のように呼び出すと 0.0 から 1.0 までの値が取得できます。1/2の確率で何か行いたい場合はmRandom.nextFloat() > 0.5のように条件判断を行います。
    // ステージを作成する
    private void createStage() {
        // テクスチャの準備
        Texture stepTexture = new Texture("step.png");
        Texture starTexture = new Texture("star.png");
        Texture enemyTexture = new Texture("enemy.png");
        Texture playerTexture = new Texture("uma.png");
        Texture ufoTexture = new Texture("ufo.png");

        // StepとStarとEnemyをゴールの高さまで配置していく
        float y = 0;

        float maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_JUMP_VELOCITY / (2 * -GRAVITY);
        while (y < WORLD_HEIGHT - 5) {
            int type = mRandom.nextFloat() > 0.8f ? Step.STEP_TYPE_MOVING : Step.STEP_TYPE_STATIC;
            //int etype = mRandom.nextFloat() > 0.9f ? Enemy.ENEMY_TYPE_MOVING : Enemy.ENEMY_TYPE_STATIC;
            float x = mRandom.nextFloat() * (WORLD_WIDTH - Step.STEP_WIDTH);

            Step step = new Step(type, stepTexture, 0, 0, 144, 36);
            step.setPosition(x, y);
            mSteps.add(step);

            if (mRandom.nextFloat() > 0.6f) {
                Star star = new Star(starTexture, 0, 0, 72, 72);
                star.setPosition(step.getX() + mRandom.nextFloat(), step.getY() + Star.STAR_HEIGHT + mRandom.nextFloat() * 3);
                mStars.add(star);
            }

            //Enemyを配置
            if (mRandom.nextFloat() > 0.8f) {
                Enemy enemy = new Enemy(Enemy.ENEMY_TYPE_MOVING,enemyTexture, 0, 0, 72, 72);
                enemy.setPosition(x + mRandom.nextFloat() * 2, y + mRandom.nextFloat() * 8);
                mEnemy.add(enemy);
            }

            y += (maxJumpHeight - 0.5f);
            y -= mRandom.nextFloat() * (maxJumpHeight / 3);
        }

        // Playerを配置
        mPlayer = new Player(playerTexture, 0, 0, 72, 72);
        mPlayer.setPosition(WORLD_WIDTH / 2 - mPlayer.getWidth() / 2, Step.STEP_HEIGHT);

        // ゴールのUFOを配置
        mUfo = new Ufo(ufoTexture, 0, 0, 120, 74);
        mUfo.setPosition(WORLD_WIDTH / 2 - Ufo.UFO_WIDTH / 2, y);
    }

    /*
    ゲーム開始前のupdateReadyメソッドではタッチされたら状態をゲーム中であるGAME_STATE_PLAYINGに変更します。
    ゲーム中の updatePlayingメソッドではタッチされていたら、その座標が画面の左側なのか右側なのかを判断します。
    タッチされた座標はGdx.input.getX()とGdx.input.getY()で取得することができます。
    それらの値をmTouchPointにsetメソッドで設定します。
    Vector3クラスはx,yだけでなくZ軸を保持するメンバ変数zも持っているためsetメソッドの第3引数には0を指定しています。
    そしてそのmTouchPointをOrthographicCameraクラスのunprojectメソッドに与えて呼び出すことでカメラを使った座標に変換します。
    画面のどこをタッチされたかの判断はRectangleクラスの左半分を表す矩形leftと右半分を表す矩形rightを定義し、containsメソッドにタッチされた座標を与えることでその領域をタッチしているのか判断します。左側をタッチされた時は加速度としてaccel = 5.0fと、右側を タッチされたときはaccel = -5.0fを設定します。
    そしてその加速度をPlayerクラスのupdateメソッドに与えて呼び出します。踏み台の状態も更新させるためStepクラスのupdateメソッドも呼び出します。また、プレイヤーの座標が0.5以下になった場合は踏み台に乗ったと同じ処理(hitStepメソッド)を行い、ジャンプさせます。これはゲーム開始時にジャンプさせるための処理です。
    updatePlayingメソッドの最後はプレイヤーがどれだけ地面から離れたかを、Mathクラスのmaxメソッドを呼び出して保持している距離か、今のプレイヤーの高さか大きい方を保持します。
     */
    // それぞれのオブジェクトの状態をアップデートする
    private void update(float delta) {
        switch (mGameState) {
            case GAME_STATE_READY:
                updateReady();
                break;
            case GAME_STATE_PLAYING:
                updatePlaying(delta);
                break;
            case GAME_STATE_GAMEOVER:
                updateGameOver();
                break;
        }
    }

    private void updateReady() {
        if (Gdx.input.justTouched()) {
            mGameState = GAME_STATE_PLAYING;
            bgmid = bgm_sound.play(1.0f);
            bgm_sound.setLooping(bgmid,true);
        }
    }

    private void updatePlaying(float delta) {

        float accel = 0;
        if (Gdx.input.isTouched()) {
            mGuiViewPort.unproject(mTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));
            Rectangle left = new Rectangle(0, 0, GUI_WIDTH / 2, GUI_HEIGHT);
            Rectangle right = new Rectangle(GUI_WIDTH / 2, 0, GUI_WIDTH / 2, GUI_HEIGHT);
            if (left.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = 5.0f;
            }
            if (right.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = -5.0f;
            }
        }

        // Enemy
        for (int i = 0; i < mEnemy.size(); i++) {
            mEnemy.get(i).update(delta);
        }

        // Step
        for (int i = 0; i < mSteps.size(); i++) {
            mSteps.get(i).update(delta);
        }

        // Player
        if (mPlayer.getY() <= Player.PLAYER_HEIGHT / 2) {
            mPlayer.hitStep();
        }
        mPlayer.update(delta, accel);
        mHeightSoFar = Math.max(mPlayer.getY(), mHeightSoFar);

        // 当たり判定を行う
        checkCollision();

        // ゲームオーバーか判断する
        checkGameOver();
    }

    /*
    次にcheckCollisionメソッドを実装します。当たり判定を行うには当たり判定を行うオブジェクト（スプライト）の矩形同士が重なっているかを判断し、重なっていれば当たっていると判断します。SpriteクラスのgetBoundingRectangleメソッドでスプライトの矩形を表すRectangleを取得します。各オブジェクトのクラスはGameObjectクラスを継承しており、GameObjectクラスはSpriteクラスを継承しているのでgetBoundingRectangleメソッドを呼び出すことができます。Rectangleクラスのoverlapsメソッドに当たり判定を行いたい相手のRectangleを指定します。戻り値がtrueであれば重なっている＝当たっていることになります。
    UFOと当たった場合はゲームクリアとなるので状態をGAME_STATE_GAMEOVERにしてメソッドを抜けます。
    星との当たり判定は相手となるStarクラスのmStateがStar.STAR_NONEの場合はすでに当たって獲得済みなので当たり判定を行いません。踏み台との当たり判定はプレイヤーが上昇中＝mPlayer.velocity.y > 0の時は行いません。
    踏み台との当たり判定はstep.mState == Step.STEP_STATE_VANISH以外のもので判定を行います。踏み台と当たった場合はmRandom.nextFloat() > 0.5fで判断して、つまり1/2の確率で踏み台を消します。ゲームバランスを調整する際はこの値も変更してみると良いでしょう。
     */

    private void checkCollision() {
        // UFO(ゴールとの当たり判定)
        if (mPlayer.getBoundingRectangle().overlaps(mUfo.getBoundingRectangle())) {
            mGameState = GAME_STATE_GAMEOVER;
            return;
        }
        // 敵との当たり判定
        for (int i = 0; i < mEnemy.size(); i++) {
            Enemy enemy = mEnemy.get(i);
            if (enemy.mState == Enemy.ENEMY_STATE_VANISH) {
                continue;
            }
            if (mPlayer.getY() > enemy.getY()) {
                if (mPlayer.getBoundingRectangle().overlaps(enemy.getBoundingRectangle())) {
                    //bgm_sound.dispose();
                    bgm_sound.stop();

                    // GameOver時に音源再生
                    sound.play(1.0f);
                    sound.dispose();
                    mGameState = GAME_STATE_GAMEOVER;
                    break;
                }
            }
        }

        // Starとの当たり判定
        for (int i = 0; i < mStars.size(); i++) {
            Star star = mStars.get(i);

            if (star.mState == Star.STAR_NONE) {
                continue;
            }


            if (mPlayer.getBoundingRectangle().overlaps(star.getBoundingRectangle())) {
                star.get();
                mScore++;
                if (mScore > mHighScore) {
                    mHighScore = mScore;
                    //ハイスコアをPreferenceに保存する
                    mPrefs.putInteger("HIGHSCORE", mHighScore); // ←追加する
                    mPrefs.flush(); // ←追加する
                }
                break;
            }
        }

        // Stepとの当たり判定
        // 上昇中はStepとの当たり判定を確認しない
        if (mPlayer.velocity.y > 0) {
            return;
        }

        for (int i = 0; i < mSteps.size(); i++) {
            Step step = mSteps.get(i);

            if (step.mState == Step.STEP_STATE_VANISH) {
                continue;
            }

            if (mPlayer.getY() > step.getY()) {
                if (mPlayer.getBoundingRectangle().overlaps(step.getBoundingRectangle())) {
                    mPlayer.hitStep();
                    if (mRandom.nextFloat() > 0.5f) {
                        step.vanish();
                    }
                    break;
                }
            }
        }
    }

    private void checkGameOver() {
        if (mHeightSoFar - CAMERA_HEIGHT / 2 > mPlayer.getY()) {
            Gdx.app.log("JampActionGame", "GAMEOVER");
            bgm_sound.stop();
            mGameState = GAME_STATE_GAMEOVER;

        }
    }

    private void updateGameOver() {
        if (Gdx.input.justTouched()) {
            mGame.setScreen(new ResultScreen(mGame, mScore));
            bgm_sound.stop();
        }
    }
}