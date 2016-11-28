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
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

// ----------------------------------------------------------------------

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    Camera mCamera;
    SurfaceView mSurface;
    SurfaceHolder mHolder;
    private boolean hasSurface = false;
    private int mPreviewWidth;
    private int mPreviewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        setContentView(R.layout.activity_main);

        mSurface = (SurfaceView) findViewById(R.id.preview);
        mHolder = mSurface.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);
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
        CameraManager.getInstance().doOpenCamera();
        CameraManager.getInstance().doStartPreview(mHolder, mSurface.getWidth(), mSurface
                .getHeight());
        updatePreviewLayout(mSurface.getWidth(), mSurface.getHeight());

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
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
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
}
