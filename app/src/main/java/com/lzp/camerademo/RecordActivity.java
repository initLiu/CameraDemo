package com.lzp.camerademo;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

/**
 * Created by SKJP on 2016/12/4.
 */

public class RecordActivity extends Activity implements SurfaceHolder.Callback, View
        .OnClickListener {
    private static final int VIDEO_SIZE_WIDTH = 640;  //视频大小640*480
    private static final int VIDEO_SIZE_HEIGHT = 480;
    private static final int DEFAULT_VIDEO_FRAME_RATE = 30; //视频帧速率 30fps
    private static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100; //音频采样率 44kHZ
    private static final int DEFAULT_VIDEO_BIT_RATE = 640000; //视频比特率 625k bps
    private static final int DEFAULT_AUDIO_BIT_RATE = 64000; //音频比特率 64k bps
    private static final int DEFAULT_AUDIO_CHANNEL = 1; //1声道

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button btnRecord;

    private int backCameraID, frontCameraId, defaultCameraId;
    private boolean hasSurface = false;
    private boolean isPause, isRecording;
    private MediaRecorder mRecorder;
    private File mOutputFile;
    private String mOutVideoPath;

    private Camera mCamera;
    private MediaPrepareTask mMediaPrepareTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.record_activity);

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

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

    @Override
    protected void onPause() {
        super.onPause();
        isPause = true;
        if (isRecording) {
            mRecorder.stop();
            isRecording = false;
        }
        mMediaPrepareTask = null;
        releaseMediaRecorder();
        CameraManager.getInstance().doStopCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        hasSurface = true;
        openCameraRunnable.run();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        startRecord();
    }

    private void startRecord() {
        if (!isRecording) {
            mMediaPrepareTask = new MediaPrepareTask();
            mMediaPrepareTask.execute(null, null, null);
        } else {
            mRecorder.stop();
            isRecording = false;
            releaseMediaRecorder();
        }
    }

    private Runnable openCameraRunnable = new Runnable() {
        @Override
        public void run() {
            startOpenCamera();
        }
    };

    private void startOpenCamera() {
        CameraManager.getInstance().setRecord(true);
        CameraManager.getInstance().doOpenCamera(defaultCameraId);
        CameraManager.getInstance().doStartPreview(this, mSurfaceHolder, mSurfaceView.getWidth(),
                mSurfaceView
                        .getHeight());
        updatePreviewLayout(mSurfaceView.getWidth(), mSurfaceView.getHeight());
        mCamera = CameraManager.getInstance().getCamera();
    }

    //设置预览Frame的尺寸，包含preview和遮罩层
    private void updatePreviewLayout(int width, int height) {
        ViewGroup.LayoutParams params = mSurfaceView.getLayoutParams();
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
        mSurfaceView.setLayoutParams(params);
    }

    private boolean prepareVideoRecorder() {
        mRecorder = new MediaRecorder();
        mCamera.unlock();
        mRecorder.setCamera(mCamera);

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //https://developer.android.com/guide/appendix/media-formats.html
        mRecorder.setVideoSize(VIDEO_SIZE_WIDTH, VIDEO_SIZE_HEIGHT);
        mRecorder.setVideoFrameRate(DEFAULT_VIDEO_FRAME_RATE);
        mRecorder.setAudioSamplingRate(DEFAULT_AUDIO_SAMPLE_RATE);//采样率，44100Hz是唯一可以保证兼容所有Android手机的采样率
        mRecorder.setAudioEncodingBitRate(DEFAULT_AUDIO_BIT_RATE);
        mRecorder.setAudioChannels(DEFAULT_AUDIO_CHANNEL);
        if (Build.VERSION.SDK_INT > 7) {
            mRecorder.setVideoEncodingBitRate(DEFAULT_VIDEO_BIT_RATE);
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cameardemo" +
                ".mp4";
        mRecorder.setOutputFile(path);
        mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try {
            mRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            CameraManager.getInstance().getCamera().lock();
        }
    }

    private class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            if (prepareVideoRecorder()) {
                if (!isPause) {
                    isRecording = true;
                    try {
                        mRecorder.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            releaseMediaRecorder();
                            return false;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } else {
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (!aBoolean) {
                RecordActivity.this.finish();
            }
        }
    }
}
