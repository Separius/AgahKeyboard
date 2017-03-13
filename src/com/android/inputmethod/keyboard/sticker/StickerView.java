package com.android.inputmethod.keyboard.sticker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.android.inputmethod.latin.utils.ResourceUtils;

import java.util.Arrays;
import java.util.List;

import io.separ.neural.inputmethod.colors.ColorManager;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 3/5/17.
 */
public class StickerView extends LinearLayout implements ColorManager.OnColorChange{
    private LinearLayout mStickerTopBar;
    private TabHost mTabHost;
    private ViewPager mStickerPager;
    private StickerPagerAdapter mStickerPalettesAdapter;

    private final int mCategoryIndicatorDrawableResId;
    private final int mCategoryIndicatorBackgroundResId;
    private final int mCategoryPageIndicatorBackground;

    public static final List<StickerPageModel> PAGES = Arrays.asList(
            new StickerPageModel("gopher", R.attr.iconEmojiCategory1Tab, new String[] {"angry.png", "ok.png", "sigh.png", "no.png", "hot.png", "ninja.png", "good_morning.png", "balloon.png", "thank_you.png", "work.png", "run_away.png", "hot_spring.png", "scare.png", "beer.png", "tehepero.png", "hide_away.png", "awake.png", "hungry.png", "baseball.png", "hide.png", "cry.png", "hi.png", "lovely.png", "cook.png", "cold.png", "cheer.png", "faint.png", "sleepy.png", "sorry.png", "spring.png", "embarrass.png", "bye.png", "question.png", "autumn.png", "surprise.png", "sleeping.png"})
    );

    /*private void addTab(final TabHost host, final StickerPageModel currentPage) {
        final TabHost.TabSpec tspec = host.newTabSpec(currentPage.getName());
        tspec.setContent(R.id.sticker_keyboard_dummy);
        final ImageView iconView = (ImageView) LayoutInflater.from(getContext()).inflate(
                R.layout.sticker_keyboard_tab_icon, null);
        //iconView.setBackgroundColor(mCategoryPageIndicatorBackground);
        Glide.with(getContext())
                .load(Uri.parse("file:///android_asset/stickers/"+currentPage.getName()+"/"+currentPage.getPack()[0]))
                .override(50, 50)
                .fitCenter()
                .into(iconView);
        //iconView.setImageResource(getDrawableId(currentPage.getName()));//TODO
        tspec.setIndicator(iconView);
        host.addTab(tspec);
    }*/

    @Override
    protected void onFinishInflate() {
        /*mStickerTopBar = (LinearLayout)findViewById(R.id.sticker_top_bar);
        mTabHost = (TabHost)findViewById(R.id.sticker_category_tabhost);
        mTabHost.setup();
        for (StickerPageModel i : PAGES)
            addTab(mTabHost, i);
        mTabHost.setOnTabChangedListener(this);//TODO
        final TabWidget tabWidget = mTabHost.getTabWidget();
        tabWidget.setStripEnabled(true);
        tabWidget.setBackgroundResource(mCategoryIndicatorDrawableResId);*/
//        tabWidget.setLeftStripDrawable(mCategoryIndicatorBackgroundResId); //TODO
//        tabWidget.setRightStripDrawable(mCategoryIndicatorBackgroundResId);

        mStickerPalettesAdapter = new StickerPagerAdapter(getContext());
                /*new EmojiPageView.EmojiSelectionListener() {
                    @Override
                    public void onEmojiSelected(String emoji) {
                        mKeyboardActionListener.onEmojiInput(emoji);
                    }
                });*/

        mStickerPager = (ViewPager)findViewById(R.id.sticker_keyboard_pager);
        mStickerPager.setAdapter(mStickerPalettesAdapter);
        //mStickerPager.setOnPageChangeListener(this);//TODO
        mStickerPager.setOffscreenPageLimit(0);
        mStickerPager.setPersistentDrawingCache(PERSISTENT_NO_CACHE);
        //setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true /* force */);//TODO
    }

    public static class StickerPagerAdapter extends PagerAdapter
    {
        private Context                context;
        //private EmojiPageView.EmojiSelectionListener listener;

        public StickerPagerAdapter(@NonNull Context context)
                                 //@Nullable EmojiPageView.EmojiSelectionListener listener)
        {
            super();
            this.context  = context;
            //this.listener = listener;
        }

        @Override
        public CharSequence getPageTitle(int index){
            return "Text";
        }

        @Override
        public int getCount() {
            return PAGES.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            StickerPageView page = new StickerPageView(context);
            page.setModel(PAGES.get(position));
            //page.setStickerSelectedListener(listener);
            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            //StickerPageView current = (StickerPageView) object;
            //current.onSelected();
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void updateColor(int primary) {

        }
    }

    public void onColorChange(ColorProfile newProfile){
        int primary = newProfile.getPrimary();
        int secondary = newProfile.getSecondary();
        int iconColor = newProfile.getIconOnSecondary();
        if(mTabHost != null){
            TabWidget tabWidget = mTabHost.getTabWidget();
            for(int i=0; i<tabWidget.getChildCount(); ++i) {
                ImageView currentTab = (ImageView)tabWidget.getChildTabViewAt(i);
                currentTab.setBackgroundColor(secondary);
                currentTab.setColorFilter(iconColor);
            }
            tabWidget.setBackgroundColor(secondary);
        }
        if(mStickerTopBar != null)
            mStickerTopBar.setBackgroundColor(secondary);
        mStickerPager.setBackgroundColor(primary);
        mStickerPalettesAdapter.updateColor(primary);
    }

    public StickerView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.emojiPalettesViewStyle);
    }

    public StickerView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray emojiPalettesViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.EmojiPalettesView, defStyle, R.style.EmojiPalettesView);
        mCategoryIndicatorDrawableResId = emojiPalettesViewAttr.getResourceId(
                R.styleable.EmojiPalettesView_categoryIndicatorDrawable, 0);
        mCategoryIndicatorBackgroundResId = emojiPalettesViewAttr.getResourceId(
                R.styleable.EmojiPalettesView_categoryIndicatorBackground, 0);
        mCategoryPageIndicatorBackground = emojiPalettesViewAttr.getColor(
                R.styleable.EmojiPalettesView_categoryPageIndicatorBackground, 0);
        emojiPalettesViewAttr.recycle();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources res = getContext().getResources();
        // The main keyboard expands to the entire this {@link KeyboardView}.
        final int width = ResourceUtils.getDefaultKeyboardWidth(res)
                + getPaddingLeft() + getPaddingRight();
        final int height = ResourceUtils.getDefaultKeyboardHeight(res)
                // res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }
}
