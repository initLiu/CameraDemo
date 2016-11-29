package com.lzp.camerademo;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SKJP on 2016/11/28.
 */

public class CameraManager {

    private static final int MAX_PICTURE_HEIGHT = 1080; //生成的图片尺寸与此最接近
    private static CameraManager mCameraInterface;
    private boolean isPreviewing = false;

    private AutoFocusManager mAutoFocusManager;
    private Camera mCamera;
    private int mCameraID;

    private CameraManager() {

    }

    public static synchronized CameraManager getInstance() {
        if (mCameraInterface == null) {
            mCameraInterface = new CameraManager();
        }
        return mCameraInterface;
    }

    public int getCameraID() {
        return mCameraID;
    }

    public void doOpenCamera(int cameraId) {
        mCameraID = cameraId;
        mCamera = Camera.open(cameraId);
    }

    public void doStartPreview(Activity activity, SurfaceHolder holder, int width, int height) {
        if (isPreviewing) {
            mCamera.stopPreview();
            return;
        }
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            initCamera(activity, width, height);
        }
    }

    private void initCamera(Activity activity, int previewWidth, int previewHeight) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
            Camera.Size optimalPreSize = getOptimalPreviewSize(supportedPreviewSizes, Math.max
                    (previewWidth, previewHeight), Math.min(previewWidth, previewHeight));

            Camera.Size optimalPicSize = getOptimalPictureSize(supportedPictureSizes,
                    optimalPreSize);
            //设置相机参数
            try {
                parameters.setPreviewSize(optimalPreSize.width, optimalPreSize.height);
                parameters.setPictureSize(optimalPicSize.width, optimalPicSize.height);
                parameters.setPictureFormat(PixelFormat.JPEG);
                parameters.setJpegQuality(100);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }

//            mCamera.setDisplayOrientation(90);
            setCameraDisplayOrientation(activity, mCameraID, mCamera);
            mCamera.startPreview();
            mAutoFocusManager = new AutoFocusManager(mCamera);
            isPreviewing = true;
        }
    }

    public void setCameraDisplayOrientation(Activity activity,
                                            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    //根据预览尺寸获取相同比例的图片尺寸,选择最适合的尺寸
    private Camera.Size getOptimalPictureSize(List<Camera.Size> pictureSizes, Camera.Size
            previewSize) {
        Camera.Size optimalSize = null;
        int targetHeight = MAX_PICTURE_HEIGHT;
        double minDiff = Double.MAX_VALUE;

        double targetRation = (double) previewSize.width / previewSize.height;
        for (Camera.Size size : pictureSizes) {
            double ration = (double) size.width / size.height;
            if (ration == targetRation) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : pictureSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 根据预设宽高获取匹配的相机分辨率,若没有则返回中间值
     * 注意调用时需要保证w>h,才可以通过该方法找到最合适的size
     *
     * @param previewSizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> previewSizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRation = (double) w / h;
        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        List<Camera.Size> filterPreviewSize = getFilterSize(previewSizes, false);
        for (Camera.Size size : filterPreviewSize) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRation) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : filterPreviewSize) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /* 获取大部分手机支持的分辨率比例 4：3 或 16：9 (将一些奇葩比例的分辨率给过滤掉)，
     * 如果没有正常比例（4：3或16：9）的分辨率，则不进行过滤
     * 在录制视频时，因video尺寸是4：3的，所以只要4：3的尺寸，因为有些手机如果不设置同样的尺寸，点击录制时会变形
     */
    public List<Camera.Size> getFilterSize(List<Camera.Size> orignSize, boolean isOnly43) {
        List<Camera.Size> filterSizes = new ArrayList<>();
        for (Camera.Size size : orignSize) {
            double ratio = (double) size.height / size.width;
            if (ratio == 0.75) {//4:3
                filterSizes.add(size);
            }
            if (!isOnly43 && ratio == 0.5625f) {//16:9
                filterSizes.add(size);
            }
        }
        if (filterSizes.size() == 0) {
            return orignSize;
        }
        return filterSizes;
    }

    public Camera.Size doGetPreviewSize() {
        return mCamera.getParameters().getPreviewSize();
    }

    public Camera.Size doGetPictureSize() {
        return mCamera.getParameters().getPictureSize();
    }

    public void doStopCamera() {
        if (mAutoFocusManager != null) {
            mAutoFocusManager.stop();
            mAutoFocusManager = null;
        }

        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
    }

    public void doTakePicture(Camera.PictureCallback callback) {
        if (null != mCamera) {
            mCamera.takePicture(null, null, callback);
        }
    }
}
