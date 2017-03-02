package com.android.inputmethod.keyboard.top;

import android.view.View;
import android.widget.LinearLayout;

import com.android.inputmethod.keyboard.top.actionrow.ActionRowView;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.suggestions.SuggestionStripView;

/**
 * Created by sepehr on 3/2/17.
 */

public class TopDisplayController {
    final Runnable hideSuggestionAfter;
    private final LinearLayout holderLayout; //contains SStrip, ActionRow(nums,emojis,serviceList), ServiceResultsView
    private final ActionRowView mActionRowView;
    private final View mSuggestionsStripHackyContainer;
    private final SuggestionStripView mSuggestionsStripView;

    class C04611 implements Runnable {
        C04611() {
        }
        public void run() {
            mActionRowView.setVisibility(View.VISIBLE);
            mSuggestionsStripHackyContainer.setVisibility(View.GONE);
        }
    }

    TopDisplayController(View parent){
        hideSuggestionAfter = new C04611();
        holderLayout = (LinearLayout) parent.findViewById(R.id.keyboard_top_area);
        mActionRowView = (ActionRowView) parent.findViewById(R.id.action_row);
        mSuggestionsStripView = (SuggestionStripView) parent.findViewById(R.id.suggestion_strip_view);
        mSuggestionsStripHackyContainer = parent.findViewById(R.id.suggestion_strip_hacky_container);
        mSuggestionsStripHackyContainer.setVisibility(View.GONE);
    }
}
