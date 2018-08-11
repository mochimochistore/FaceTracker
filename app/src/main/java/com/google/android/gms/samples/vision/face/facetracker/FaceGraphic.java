/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    private Bitmap mImage;

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);

        // 顔認識 1件ごとに、当クラスのインスタンスが 1つ生成される

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        // ウサギの耳のイメージを準備する
        mImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.bunny_ears);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate(); // postInvalidate メソッドを呼び出すことで、結果的に draw メソッドが呼び出される
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // 認識した顔領域の中心に円（点）を描画
        // （ translateX、translateY メソッドによって、フロントカメラ、リアカメラの場合の座標の差異を吸収している）
        float centerX = translateX(face.getPosition().x + face.getWidth() / 2);
        float centerY = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(centerX, centerY, FACE_POSITION_RADIUS, mFacePositionPaint);

        // 認識した顔領域の情報を元に、その顔領域を覆う長方形を描画
        // （ scaleX、scaleY メソッドは、それぞれ、取得した座標を、実際の画面に合わせて調整するメソッド）
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = centerX - xOffset;
        float top = centerY - yOffset;
        float right = centerX + xOffset;
        float bottom = centerY + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        // 目、口などを検知した位置の情報を取得
        List<Landmark> landmarks = face.getLandmarks();

        // 目、口などの位置の情報を保持する Map を作成
        Map<Integer, PointF> positions = new HashMap<>();

        for (Landmark landmark : landmarks) {
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:
                    // 左目を検知した場合
                    positions.put(Landmark.LEFT_EYE, landmark.getPosition());
                    break;
                case Landmark.RIGHT_EYE:
                    // 右目を検知した場合
                    positions.put(Landmark.RIGHT_EYE, landmark.getPosition());
                    break;
                case Landmark.LEFT_MOUTH:
                    // 口の左端を検知した場合
                    positions.put(Landmark.LEFT_MOUTH, landmark.getPosition());
                    break;
                case Landmark.RIGHT_MOUTH:
                    // 口の右端を検知した場合
                    positions.put(Landmark.RIGHT_MOUTH, landmark.getPosition());
                    break;
                case Landmark.BOTTOM_MOUTH:
                    // 口の下端を検知した場合
                    positions.put(Landmark.BOTTOM_MOUTH, landmark.getPosition());
                    break;
            }
        }

        // 左目位置に四角形を描画
        if (positions.containsKey(Landmark.LEFT_EYE)) {
            float x = translateX(positions.get(Landmark.LEFT_EYE).x);
            float y = translateY(positions.get(Landmark.LEFT_EYE).y);
            float offset = face.getWidth() / 5.0f;
            canvas.drawRect(x - offset, y - offset, x + offset, y + offset, mBoxPaint);
        }
        // 右目位置に四角形を描画
        if (positions.containsKey(Landmark.RIGHT_EYE)) {
            float x = translateX(positions.get(Landmark.RIGHT_EYE).x);
            float y = translateY(positions.get(Landmark.RIGHT_EYE).y);
            float offset = face.getWidth() / 5.0f;
            canvas.drawRect(x - offset, y - offset, x + offset, y + offset, mBoxPaint);
        }
        // 口の位置に四角形を描画
        if (positions.containsKey(Landmark.LEFT_MOUTH) &&
                positions.containsKey(Landmark.RIGHT_MOUTH) &&
                positions.containsKey(Landmark.BOTTOM_MOUTH)) {
            float mouthLeft = translateX(positions.get(Landmark.LEFT_MOUTH).x);
            float mouthTop = (positions.get(Landmark.LEFT_MOUTH).y > positions.get(Landmark.RIGHT_MOUTH).y)
                    ? translateY(positions.get(Landmark.LEFT_MOUTH).y) : translateY(positions.get(Landmark.RIGHT_MOUTH).y);
            float mouthRight = translateX(positions.get(Landmark.RIGHT_MOUTH).x);
            float mouthBottom = translateY(positions.get(Landmark.BOTTOM_MOUTH).y);
            canvas.drawRect(mouthLeft, mouthTop, mouthRight, mouthBottom, mBoxPaint);
        }

        // 笑顔の度合いを 0.00 - 1.00 の数値で表示
        canvas.drawText("笑顔レベル: " + String.format("%.2f", face.getIsSmilingProbability()),
                left - ID_X_OFFSET, bottom - ID_Y_OFFSET, mIdPaint);

        // 笑顔の度合いが一定レベルを超えたら、ウサギの耳のイメージを頭の位置の上に描画
        if (face.getIsSmilingProbability() > 0.3f) {
            Rect original = new Rect(0, 0, mImage.getWidth(), mImage.getHeight());

            float offsetBottom = scaleY(face.getHeight() / 4.0f);
            float offsetTop = offsetBottom + (right - left); // 正方形になるように描画
            RectF destination = new RectF(left, centerY - offsetTop, right, centerY - offsetBottom);

            canvas.drawBitmap(mImage, original, destination, new Paint());
        }

//        // Draws a circle at the position of the detected face, with the face's track id below.
//        float x = translateX(face.getPosition().x + face.getWidth() / 2);
//        float y = translateY(face.getPosition().y + face.getHeight() / 2);
//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
//        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
//        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);

        // Draws a bounding box around the face.
//        float xOffset = scaleX(face.getWidth() / 2.0f);
//        float yOffset = scaleY(face.getHeight() / 2.0f);
//        float left = x - xOffset;
//        float top = y - yOffset;
//        float right = x + xOffset;
//        float bottom = y + yOffset;
//        canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }
}
