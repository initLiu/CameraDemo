package com.lzp.camerademo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by SKJP on 2016/11/29.
 */

public class MarkView extends View {

    private int mBackgroundColor;
    private int mCropLineColor;
    private Rect mCenterRect;
    private Paint mBackPain, mCropPain;
    private Context mContext;

    private static final int CENTER_RECT_LINE_HEIGHT = 80; //拍摄框线高80px

    public MarkView(Context context) {
        this(context, null);
    }

    public MarkView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mBackgroundColor = getResources().getColor(R.color.sdk_black_30_color);
        mCropLineColor = getResources().getColor(R.color.sdk_gray_60_color);

        mBackPain = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackPain.setColor(mBackgroundColor);
        mBackPain.setStyle(Paint.Style.FILL);

        mCropPain = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCropPain.setColor(mCropLineColor);
        mCropPain.setStrokeWidth(dip2px(2));
        mCropPain.setAlpha(102);
    }

    public void setCenterRect(Rect rect) {
        mCenterRect = rect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCenterRect != null) {
            canvas.drawRect(0, 0, getRight(), mCenterRect.top, mBackPain);
            canvas.drawRect(0, mCenterRect.bottom, getRight(), getBottom(), mBackPain);
            canvas.drawRect(0, mCenterRect.top, mCenterRect.left, mCenterRect.bottom, mBackPain);
            canvas.drawRect(mCenterRect.right, mCenterRect.top, getRight(), mCenterRect.bottom,
                    mBackPain);

            canvas.drawLine(mCenterRect.left, mCenterRect.top, mCenterRect.left +
                    CENTER_RECT_LINE_HEIGHT, mCenterRect.top, mCropPain);
            canvas.drawLine(mCenterRect.left, mCenterRect.top, mCenterRect.left, mCenterRect.top
                    + CENTER_RECT_LINE_HEIGHT, mCropPain);
            canvas.drawLine(mCenterRect.right - CENTER_RECT_LINE_HEIGHT, mCenterRect.top,
                    mCenterRect.right, mCenterRect.top, mCropPain);
            canvas.drawLine(mCenterRect.right, mCenterRect.top,
                    mCenterRect.right, mCenterRect.top + CENTER_RECT_LINE_HEIGHT, mCropPain);
            canvas.drawLine(mCenterRect.left, mCenterRect.bottom - CENTER_RECT_LINE_HEIGHT,
                    mCenterRect.left, mCenterRect.bottom, mCropPain);
            canvas.drawLine(mCenterRect.left, mCenterRect.bottom, mCenterRect.left +
                    CENTER_RECT_LINE_HEIGHT, mCenterRect.bottom, mCropPain);
            canvas.drawLine(mCenterRect.right, mCenterRect.bottom - CENTER_RECT_LINE_HEIGHT,
                    mCenterRect.right, mCenterRect.bottom, mCropPain);
            canvas.drawLine(mCenterRect.right - CENTER_RECT_LINE_HEIGHT, mCenterRect.bottom,
                    mCenterRect.right, mCenterRect.bottom, mCropPain);
        }
        super.onDraw(canvas);
    }

    public int dip2px(float dipValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
