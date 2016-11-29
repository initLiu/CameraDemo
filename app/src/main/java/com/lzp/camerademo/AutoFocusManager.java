package com.lzp.camerademo;

import android.hardware.Camera;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

/**
 * 每个2s进行一次自动对焦
 * Created by SKJP on 2016/11/29.
 */

public class AutoFocusManager implements Camera.AutoFocusCallback {
    private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;
    private static final Collection<String> FOCUS_MODES_CALLING_AF;

    static {
        FOCUS_MODES_CALLING_AF = new ArrayList<>(2);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
    }

    private boolean stopped;
    private boolean focusing;
    private final boolean useAutoFocus;
    private final Camera camera;
    private AsyncTask<?, ?, ?> outstandingTask;

    public AutoFocusManager(Camera camera) {
        this.camera = camera;
        String currentFocusMode = camera.getParameters().getFocusMode();
        useAutoFocus = FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
        start();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        focusing = false;
        autoFocusAgainLater();
    }

    private synchronized void autoFocusAgainLater() {
        if (!stopped && outstandingTask == null) {
            AutoFocusTask newTask = new AutoFocusTask();
            try {
                newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                outstandingTask = newTask;
            } catch (RejectedExecutionException ree) {
                ree.printStackTrace();
            }
        }
    }

    /**
     * 开始自动对焦
     */
    public synchronized void start() {
        if (useAutoFocus) {
            outstandingTask = null;
            if (!stopped && !focusing) {
                try {
                    camera.autoFocus(this);
                    focusing = true;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    autoFocusAgainLater();
                }
            }
        }
    }

    private synchronized void cancelOutstandingTask() {
        if (outstandingTask != null) {
            if (outstandingTask.getStatus() != AsyncTask.Status.FINISHED) {
                outstandingTask.cancel(true);
            }
            outstandingTask = null;
        }
    }

    /**
     * 停止自动对焦
     */
    public synchronized void stop() {
        stopped = true;
        if (useAutoFocus) {
            cancelOutstandingTask();
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private final class AutoFocusTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... params) {
            try {
                Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            start();
            return null;
        }
    }
}
