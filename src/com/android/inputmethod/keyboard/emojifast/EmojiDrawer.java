package com.android.inputmethod.keyboard.emojifast;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.astuetz.PagerSlidingTabStrip;

import java.util.LinkedList;
import java.util.List;

import io.separ.neural.inputmethod.indic.R;

public class EmojiDrawer extends LinearLayout {
    private ViewPager pager;
    private List<EmojiPageModel> models;
    private PagerSlidingTabStrip strip;
    private RecentEmojiPageModel recentModel;
    private EmojiDrawerListener  drawerListener;

    public EmojiDrawer(Context context) {
        this(context, null);
    }

    public EmojiDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    private void initView() {
        final View v = LayoutInflater.from(getContext()).inflate(R.layout.emoji_drawer, this, true);
        initializeResources(v);
        initializePageModels();
        initializeEmojiGrid();
    }

    private void initializeResources(View v) {
        this.pager     = (ViewPager)            v.findViewById(R.id.emoji_pager);
        this.strip     = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    public void show(int height, boolean immediate) {
        if (this.pager == null) initView();
        setVisibility(VISIBLE);
        if (drawerListener != null) drawerListener.onShown();
    }

    public void hide(boolean immediate) {
        setVisibility(GONE);
        if (drawerListener != null) drawerListener.onHidden();
    }

    private void initializeEmojiGrid() {
        if (recentModel.getEmoji().length == 0)
            pager.setCurrentItem(1);
        strip.setViewPager(pager);
    }

    private void initializePageModels() {
        this.models = new LinkedList<>();
        this.recentModel = new RecentEmojiPageModel(getContext());
        this.models.add(recentModel);
        this.models.addAll(EmojiPages.PAGES);
    }

    public interface EmojiEventListener extends EmojiPageView.EmojiSelectionListener {
        void onKeyEvent(KeyEvent keyEvent);
    }

    public interface EmojiDrawerListener {
        void onShown();
        void onHidden();
    }
}

