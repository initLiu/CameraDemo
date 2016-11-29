/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.lzp.camerademo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

// ----------------------------------------------------------------------

public class MainActivity extends Activity implements SurfaceHolder.Callback, View
        .OnClickListener, Camera.PictureCallback {
    Camera mCamera;
    SurfaceView mSurface;
    SurfaceHolder mHolder;
    private ImageView imgCpature, imgSwitch;
    private ImageView imgShot;
    private boolean hasSurface = false;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int defaultCameraId;
    private int backCameraID, frontCameraId;
    private MarkView markView;
    private Rect mMarkCenterRect;
    private int mCenterWidth, mCenterHeight;
    private Point mRectPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

//        imgShot = (ImageView) findViewById(R.id.img);
        imgCpature = (ImageView) findViewById(R.id.capture);
        imgSwitch = (ImageView) findViewById(R.id.switchBtn);
        markView = (MarkView) findViewById(R.id.mark);
        imgSwitch.setOnClickListener(this);
        imgCpature.setOnClickListener(this);

        mSurface = (SurfaceView) findViewById(R.id.preview);
        mHolder = mSurface.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCameraID = i;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            }
        }
        defaultCameraId = backCameraID;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSurface) {
            openCameraRunnable.run();
        }
    }

    private Runnable openCameraRunnable = new Runnable() {
        @Override
        public void run() {
            startOpenCamera();
        }
    };

    private void startOpenCamera() {
        CameraManager.getInstance().doOpenCamera(defaultCameraId);
        CameraManager.getInstance().doStartPreview(this, mHolder, mSurface.getWidth(), mSurface
                .getHeight());
        updatePreviewLayout(mSurface.getWidth(), mSurface.getHeight());
        mMarkCenterRect = createMarkCenterRect();
        markView.setCenterRect(mMarkCenterRect);
    }

    private Rect createMarkCenterRect() {
        mCenterWidth = mPreviewWidth - 100;
        mCenterHeight = (int) (mCenterWidth * 0.65);
        int x1 = mPreviewWidth / 2 - mCenterWidth / 2;
        int y1 = mPreviewHeight / 2 - mCenterHeight / 2;
        int x2 = x1 + mCenterWidth;
        int y2 = y1 + mCenterHeight;

        return new Rect(x1, y1, x2, y2);
    }

    //设置预览Frame的尺寸，包含preview和遮罩层
    private void updatePreviewLayout(int width, int height) {
        ViewGroup.LayoutParams params = mSurface.getLayoutParams();
        Camera.Size previewSize = CameraManager.getInstance().doGetPreviewSize();
        double w = previewSize.width;
        double h = previewSize.height;
        //保证surface的尺寸与预览尺寸比例一致，另需注意预览尺寸是w>h，所以计算比例是w/h
        //换算时因小数点可能会存在误差，这里采用Math.round()四舍五入取整
        if (width > height) {
            params.width = Math.round((float) (height * w / h));
            params.height = height;
            if (params.width > width) {
                params.width = width;
                params.height = Math.round((float) (width * h / w));
            }
        } else {
            params.width = width;
            params.height = Math.round((float) (width * w / h));
            if (params.height > height) {
                params.height = height;
                params.width = Math.round((float) (height * h / w));
            }
        }
        mSurface.setLayoutParams(params);
        mPreviewWidth = params.width;
        mPreviewHeight = params.height;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        CameraManager.getInstance().doStopCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        hasSurface = true;
        openCameraRunnable.run();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        CameraManager.getInstance().doStopCamera();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (data != null && data.length != 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            CameraManager.getInstance().doStopCamera();
            saveBitmap(bitmap);
        }
    }

    private void saveBitmap(Bitmap bitmap) {
        //只保存矩形框以内的图片
        if (bitmap != null) {
            if (mMarkCenterRect == null) {
                mMarkCenterRect = createMarkCenterRect();
            }

            int DES_RECT_WIDTH = mRectPicture.x;
            int DES_RECT_HEIGHT = mRectPicture.y;

            Bitmap rotaBitmap = rotateBitmap(bitmap, 90.0f);
            int x = rotaBitmap.getWidth() / 2 - DES_RECT_WIDTH / 2;
            int y = rotaBitmap.getHeight() / 2 - DES_RECT_HEIGHT / 2;
            if (x < 0) {
                x = 0;
                DES_RECT_WIDTH = rotaBitmap.getWidth();
            }
            if (y < 0) {
                y = 0;
                DES_RECT_HEIGHT = rotaBitmap.getHeight();
            }

            Bitmap shot = Bitmap.createBitmap(rotaBitmap, x, y, DES_RECT_WIDTH, DES_RECT_HEIGHT);
            if (!shot.isRecycled()) {
                shot.recycle();
                shot = null;
            }
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }

    }

    private Point createPictureRect() {
        int wSavePicture = CameraManager.getInstance().doGetPictureSize().height;//图片旋转了，宽高置换
        int hSavePicture = CameraManager.getInstance().doGetPictureSize().width;
        float wRate = (float) wSavePicture / (float) mPreviewWidth;
        float hRate = (float) hSavePicture / (float) mPreviewHeight;
//        float rate = (wRate <= hRate) ? wRate : hRate;
        int wRectPicture = (int) (mCenterWidth * wRate);
        int hRectPicture = (int) (mCenterHeight * hRate);
        return new Point(wRectPicture, hRectPicture);
    }

    @Override
    public void onClick(View v) {
        if (v == imgSwitch) {
            int curCamID = CameraManager.getInstance().getCameraID();
            defaultCameraId = curCamID == backCameraID ? frontCameraId : backCameraID;
            CameraManager.getInstance().doStopCamera();
            openCameraRunnable.run();
        } else if (v == imgCpature) {
            if (mRectPicture == null) {
                mRectPicture = createPictureRect();
            }
            imgCpature.setEnabled(false);
            CameraManager.getInstance().doTakePicture(this);
        }
    }

    /**
     * 旋转图片，使图片保持正确的方向。
     *
     * @param bitmap  原始图片
     * @param degrees 原始图片的角度
     * @return Bitmap 旋转后的图片
     */
    public Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (degrees == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
        bitmap.recycle();
        bitmap = null;
        return bmp;
    }
}
