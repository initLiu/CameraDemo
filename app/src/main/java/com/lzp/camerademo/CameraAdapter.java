package com.lzp.camerademo;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SKJP on 2016/12/1.
 */

public class CameraAdapter extends PagerAdapter {
    private List<View> mViews;
    private Context mContext;
    private View.OnClickListener mClickListener;

    public CameraAdapter(Context context, View.OnClickListener clickListener) {
        mViews = new ArrayList<>();
        mContext = context;
        mClickListener = clickListener;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log.e("Test", "instantiateItem");
        View view = mViews.get(position);
        BaseHodler hodler = (BaseHodler) view.getTag();
        if (hodler == null) {
            hodler = new BaseHodler();
            if (position == 0) {
                hodler.imgBtn = (ImageView) view.findViewById(R.id.capture);

            } else if (position == 1) {
                hodler.imgBtn = (ImageView) view.findViewById(R.id.record);
            }
            hodler.imgBtn.setOnClickListener(mClickListener);
            view.setTag(hodler);
        }

        if (view.getParent() != container && position < this.getCount()) {
            container.addView(view);
        }
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Log.e("Test", "destroyItem");
        View view = (View) object;
        mViews.remove(view);
    }

    public void addViews(ArrayList<View> views) {
        mViews.addAll(views);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(Object object) {
        int pos = -1;
        for (View view : mViews) {
            if (object == view) {
                pos++;
                break;
            }
        }
        return pos;
    }

    class BaseHodler {
        public ImageView imgBtn;
    }
}
