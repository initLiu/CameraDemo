package com.lzp.camerademo;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * Created by SKJP on 2016/12/1.
 */

public class CustRadiaGroup extends RadioGroup implements ViewPager.OnPageChangeListener {
    private ViewPager mPager;

    public CustRadiaGroup(Context context) {
        super(context);
    }

    public CustRadiaGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setViewPager(ViewPager pager) {
        mPager = pager;
        mPager.addOnPageChangeListener(this);
    }

    public void syncButton() {
        addRadioButton();
    }

    private void addRadioButton() {
        if (mPager != null && mPager.getAdapter() != null) {
            int count = mPager.getAdapter().getCount();
            for (int i = 0; i < count; i++) {
                RadioButton button = createButton();
                addView(button);
            }
        }
    }

    private RadioButton createButton() {
        RadioButton button = new RadioButton(getContext());
        button.setBackgroundResource(R.drawable.radio_button);
        button.setGravity(Gravity.CENTER);

        float buttonWidthDip = 10f;
        float buttonHeightDip = 10f;

        Resources res = getContext().getResources();

        RadioGroup.LayoutParams params = new LayoutParams((int) TypedValue.applyDimension
                (TypedValue.COMPLEX_UNIT_DIP, buttonWidthDip, res.getDisplayMetrics()), (int)
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, buttonHeightDip, res
                        .getDisplayMetrics()));
        params.gravity = Gravity.CENTER;
        int space = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 3f, res.getDisplayMetrics());
        params.leftMargin = space;
        params.rightMargin = space;
        button.setLayoutParams(params);
        button.setClickable(false);
        return button;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        try {
            if (position < getChildCount()) {
                RadioButton button = (RadioButton) getChildAt(position);
                button.setChecked(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
