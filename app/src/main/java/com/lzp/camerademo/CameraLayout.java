package com.lzp.camerademo;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by SKJP on 2016/11/30.
 */

public class CameraLayout extends RelativeLayout implements View.OnClickListener {
    private ViewPager mViewPage;

    private CustRadiaGroup mRadiaGroup;

    private CameraAdapter mPagerAdapter;

    private CameraListen mCameraListener;

    public CameraLayout(Context context) {
        super(context);
    }

    public CameraLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init() {
        mViewPage = (ViewPager) findViewById(R.id.viewpage);
        mRadiaGroup = (CustRadiaGroup) findViewById(R.id.radiogroup);

        mPagerAdapter = new CameraAdapter(getContext(), this);
        mViewPage.setAdapter(mPagerAdapter);
        mPagerAdapter.addViews(initPagerViews());

        mRadiaGroup.setViewPager(mViewPage);
        mRadiaGroup.syncButton();
    }

    private ArrayList<View> initPagerViews() {
        ArrayList<View> views = new ArrayList<>();
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.take_picture, null);
        View view2 = LayoutInflater.from(getContext()).inflate(R.layout.record_video, null);
        views.add(view1);
        views.add(view2);
        return views;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void addCameraListener(CameraListen listen) {
        mCameraListener = listen;
        mRadiaGroup.addCameraListener(listen);
    }

    @Override
    public void onClick(View v) {
        if (mCameraListener == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.capture:
                mCameraListener.onCaptrure(v);
                break;
            case R.id.record:
                mCameraListener.onRecord(v);
                break;
            default:
                break;
        }
    }

    public interface CameraListen {
        void onCaptrure(View view);

        void onRecord(View view);

        void onPageChanged(int position);
    }
}
