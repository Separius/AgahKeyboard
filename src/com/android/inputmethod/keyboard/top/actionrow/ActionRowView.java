package com.android.inputmethod.keyboard.top.actionrow;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.keyboard.emojifast.EmojiView;
import com.android.inputmethod.keyboard.emojifast.RecentEmojiPageModel;

import java.util.ArrayList;
import java.util.HashMap;

import io.separ.neural.inputmethod.Utils.FontUtils;
import io.separ.neural.inputmethod.colors.ColorManager;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.AudioAndHapticFeedbackManager;
import io.separ.neural.inputmethod.indic.R;
import me.relex.circleindicator.CircleIndicator;

/**
 * Created by sepehr on 2/24/17.
 */

public class ActionRowView extends ViewPager implements ColorManager.OnColorChange, View.OnTouchListener {
    public static final String[] DEFAULT_SUGGESTED_EMOJI;
    private static final String[] NUMBER_ARRAY;
    private static final int[] SERVICE_IMAGE_IDS;
    private ActionRowAdapter adapter;
    private ColorProfile colorProfile;
    private String[] layoutToShow;
    private Listener mListener;
    private HashMap<String, LinearLayout> layouts;
    private CircleIndicator mIndicator;
    private int currentPage;
    private static final String[] DEFAULT_SERVICES;

    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /*on number clicked*/
    class AnonymousClass12 implements OnClickListener {
        final TextView val$view;
        AnonymousClass12(TextView textView) {
            this.val$view = textView;
        }
        public void onClick(View tview) {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
            mListener.onNumberClicked(this.val$view.getText().toString());
        }
    }

    /*service click handler*/
    class serviceClickListener implements OnClickListener {
        final int serviceId;
        serviceClickListener(int serviceId) {
            this.serviceId = serviceId;
        }
        public void onClick(View v) {
            mListener.onServiceClicked(DEFAULT_SERVICES[serviceId]);
        }
    }

    /* onSelectAll */
    class C02297 implements OnClickListener {
        C02297() {
        }

        public void onClick(View v) {
            mListener.onSelectAll();
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
        }
    }

    /* onCut */
    class C02308 implements OnClickListener {
        C02308() {
        }

        public void onClick(View v) {
            mListener.onCut();
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
        }
    }

    /* onCopy */
    class C02319 implements OnClickListener {
        C02319() {
        }

        public void onClick(View v) {
            mListener.onCopy();
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
        }
    }

    public interface Listener {
        void onCopy();

        void onCut();

        void onEmojiClicked(String str, boolean z);

        void onNumberClicked(String str);

        void onPaste();

        void onSelectAll();

        void onServiceClicked(String id);
    }

    private class ActionRowAdapter extends PagerAdapter {
        private ArrayList<View> views;

        private ActionRowAdapter() {
            this.views = new ArrayList();
        }

        @Override
        public int getItemPosition(Object object) {
            int index = this.views.indexOf(object);
            if (index == -1) {
                return -2;
            }
            return index;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            //View v = ActionRowView.this.createViewFromID(ActionRowView.this.layoutToShow[position % ActionRowView.this.layoutToShow.length]);
            View v = layouts.get(ActionRowView.this.layoutToShow[position % ActionRowView.this.layoutToShow.length]);
            this.views.add(v);
            container.addView(v);
            return v;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        public int getCount() {
            return ActionRowView.this.layoutToShow.length;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public View getView(int position) {
            currentPage = position;
            return (View) this.views.get(position);
        }
    }

    static {
        NUMBER_ARRAY = "1,2,3,4,5,6,7,8,9,0".split("\\s*,\\s*");
        DEFAULT_SUGGESTED_EMOJI = "\u2764,\ud83d\ude15,\ud83d\ude18,\ud83d\ude22,\ud83d\ude3b,\ud83d\ude0a,\ud83d\ude09,\ud83d\ude0d".split("\\s*,\\s*");
        SERVICE_IMAGE_IDS = new int[] {R.id.gif_service_action_button, R.id.maps_service_action_button, R.id.google_service_action_button,
                R.id.contacts_service_action_button, R.id.foursquare_service_action_button, R.id.customization_service_action_button, R.id.go_to_emoji};
        DEFAULT_SERVICES = new String[] {"giphy","maps","google","contacts","foursquare","customization","emoji"};
    }

    public void setCircleIndicator(CircleIndicator ci){
        mIndicator = ci;
        mIndicator.setViewPager(this);
    }

    public ActionRowView(Context context) {
        super(context);
        this.colorProfile = new ColorProfile();
        init();
    }

    public ActionRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.colorProfile = new ColorProfile();
        init();
    }

    public void setAdapter(ActionRowAdapter adapter) {
        super.setAdapter(adapter);
    }

    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
    }

    public void onColorChange(ColorProfile newProfile) {
        this.colorProfile = newProfile;
        setBackgroundColor(newProfile.getPrimary());
        setupLayouts();
        adapter = new ActionRowAdapter();
        setAdapter(adapter);
        invalidate();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ColorManager.addObserverAndCall(this);
    }

    private void init() {
        setBackgroundColor(Color.parseColor("#eceff1"));
        layoutToShow = ActionRowSettingsActivity.DEFAULT_LAYOUTS.split("\\s*,\\s*");
        ColorManager.addObserverAndCall(this);
    }

    private void setupLayouts(){
        layouts = new HashMap<>();
        layouts.put(ActionRowSettingsActivity.SERVCICE_ID, addServices());
        layouts.put(ActionRowSettingsActivity.EMOJI_ID, addEmojis()); //TODO update this on change
        layouts.put(ActionRowSettingsActivity.CLIP_ID, addButtons());
    }

    private View addEmptyView() {
        return new LinearLayout(getContext());
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private LinearLayout addButtons() {
        int textColor = ColorManager.getLastProfile().getTextColor();
        LinearLayout layout = (LinearLayout) View.inflate(getContext(), R.layout.clipboard_action_layout, null);
        ImageView selectAll = (ImageView) layout.findViewById(R.id.select);
        selectAll.setColorFilter(textColor);
        selectAll.setSoundEffectsEnabled(false);
        selectAll.setOnClickListener(new C02297());
        selectAll.setBackgroundResource(R.drawable.action_row_bg);
        ImageView cut = (ImageView) layout.findViewById(R.id.cut);
        cut.setColorFilter(textColor);
        cut.setSoundEffectsEnabled(false);
        cut.setOnClickListener(new C02308());
        cut.setBackgroundResource(R.drawable.action_row_bg);
        ImageView copy = (ImageView) layout.findViewById(R.id.copy);
        copy.setColorFilter(textColor);
        copy.setSoundEffectsEnabled(false);
        copy.setOnClickListener(new C02319());
        copy.setBackgroundResource(R.drawable.action_row_bg);
        ImageView paste = (ImageView) layout.findViewById(R.id.paste);
        paste.setColorFilter(textColor);
        paste.setSoundEffectsEnabled(false);
        paste.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mListener.onPaste();
                AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
            }
        });
        paste.setBackgroundResource(R.drawable.action_row_bg);
        return layout;
    }

    private LinearLayout addServices(){
        LinearLayout layout = (LinearLayout) View.inflate(getContext(), R.layout.services_action_layout, null);
        int i=0;
        for(int currentServiceViewId : SERVICE_IMAGE_IDS) {
            ImageView imageView = (ImageView) layout.findViewById(currentServiceViewId);
            imageView.setColorFilter(ColorManager.getLastProfile().getTextColor());
            imageView.setSoundEffectsEnabled(false);
            imageView.setOnClickListener(new serviceClickListener(i));
            imageView.setBackgroundResource(R.drawable.action_row_bg);
            i++;
        }
        return layout;
    }

    private LinearLayout addEmojis() {
        LinearLayout emojiLayout = new LinearLayout(getContext());
        emojiLayout.setGravity(17);
        emojiLayout.setWeightSum(1.f);
        fillEmojiLayout(emojiLayout);
        return emojiLayout;
    }

    private void fillEmojiLayout(LinearLayout emojiLayout) {
        String[] emojiArray = RecentEmojiPageModel.toReversePrimitiveArray(RecentEmojiPageModel.getPersistedCache(PreferenceManager.getDefaultSharedPreferences(getContext())));
        //String[] emojiArray = FrequentEmojiHandler.getInstance(getContext()).getMostFrequentEmojis(8).toArray(new String[0]); //TODO use this
        int i=0;
        for (String emoji : emojiArray) {
            i++;
            if(i>DEFAULT_SUGGESTED_EMOJI.length)
                break;
            emojiLayout.addView(addSingleEmoji(emoji));
        }
        for(int j=0; i<DEFAULT_SUGGESTED_EMOJI.length; i++, j++)
            emojiLayout.addView(addSingleEmoji(DEFAULT_SUGGESTED_EMOJI[j]));
    }

    class AnonymousClass11 implements OnClickListener {
        final EmojiView val$view;

        AnonymousClass11(EmojiView  emojiconTextView) {
            this.val$view = emojiconTextView;
        }

        public void onClick(View tview) {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(-15, ActionRowView.this);
            mListener.onEmojiClicked(this.val$view.getEmoji(), false);
        }
    }

    private View addSingleEmoji(String emoji){
        /*TextView view = new TextView(getContext());
        view.setTypeface(FontUtils.getTypeface("emoji"));
        view.setText(emoji);
        view.setGravity(17);
        view.setTextSize(1, 22.0f * 1.f);*/
        EmojiView view = new EmojiView(getContext(), true);
        view.setEmoji(emoji);
        view.setSoundEffectsEnabled(false);
        view.setAlpha(1.f);
        view.setOnClickListener(new AnonymousClass11(view));
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, -1, 1.f / ((float) DEFAULT_SUGGESTED_EMOJI.length)));
        view.setBackgroundResource(R.drawable.action_row_bg);
        return view;
    }

    private LinearLayout addNumbers() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setGravity(17);
        layout.setWeightSum(1.f);
        String[] numbersArray = NUMBER_ARRAY;
        for (String number : numbersArray) {
            TextView view = new TextView(getContext());
            view.setText(number);
            view.setGravity(17);
            view.setTypeface(FontUtils.getCurrentLocaleTypeface());
            view.setTextColor(this.colorProfile.getTextColor());
            view.setTextSize(1, 22.0f * 1.f);
            view.setSoundEffectsEnabled(false);
            view.setOnClickListener(new AnonymousClass12(view));
            view.setOnTouchListener(this);
            view.setLayoutParams(new LinearLayout.LayoutParams(-1, -1, 1.f / ((float) numbersArray.length)));
            view.setBackgroundResource(R.drawable.action_row_bg);
            layout.addView(view);
        }
        return layout;
    }


}
