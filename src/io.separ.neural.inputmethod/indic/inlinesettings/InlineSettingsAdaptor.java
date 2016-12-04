package io.separ.neural.inputmethod.indic.inlinesettings;

import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.separ.neural.inputmethod.indic.LatinIME;

public final class InlineSettingsAdaptor extends PagerAdapter{
    private final LatinIME mContext;

    public InlineSettingsAdaptor(LatinIME context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, final int position) {
        CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
        Button mButton = new Button(mContext);
        mButton.setText("Insert "+customPagerEnum.getTitleResId());
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.SendRichContentSample(position);
            }
        });
        layout.addView(mButton);
        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return CustomPagerEnum.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        return customPagerEnum.getTitleResId();
    }
}
