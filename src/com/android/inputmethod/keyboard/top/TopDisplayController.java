package com.android.inputmethod.keyboard.top;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.inputmethod.keyboard.top.actionrow.ActionRowView;
import com.android.inputmethod.keyboard.top.services.ServiceResultsView;

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
    private final ServiceResultsView mServiceResultsView;
    private final View mActionRowContainer;

    public int getHeight() {
        return holderLayout.getHeight();
    }

    class C04611 implements Runnable {
        C04611() {
        }
        public void run() {
            mActionRowContainer.setVisibility(View.VISIBLE);
            mSuggestionsStripHackyContainer.setVisibility(View.GONE);
        }
    }

    public void updateBarVisibility() {
        mActionRowContainer.setVisibility(View.VISIBLE);
        mSuggestionsStripHackyContainer.setVisibility(View.GONE);
    }

    public void drop() {
        //this.mServiceResultsView.drop();
    }

    public TopDisplayController(View parent){
        hideSuggestionAfter = new C04611();
        mActionRowContainer = (RelativeLayout)parent.findViewById(R.id.action_row_container);
        holderLayout = (LinearLayout) parent.findViewById(R.id.keyboard_top_area);
        mActionRowView = (ActionRowView) parent.findViewById(R.id.action_row);
        mSuggestionsStripView = (SuggestionStripView) parent.findViewById(R.id.suggestion_strip_view);
        mSuggestionsStripHackyContainer = parent.findViewById(R.id.suggestion_strip_hacky_container);
        mSuggestionsStripHackyContainer.setVisibility(View.GONE);
        mServiceResultsView = (ServiceResultsView) parent.findViewById(R.id.suggestion_source_results);
        mServiceResultsView.setVisibility(View.GONE);
    }

    public void showSuggestions() {
        if (this.mSuggestionsStripView.getVisibility() == View.VISIBLE) {
            if (this.mServiceResultsView.getVisibility() == View.VISIBLE) {
                this.mSuggestionsStripHackyContainer.setVisibility(View.GONE);
                return;
            }
            mActionRowContainer.removeCallbacks(this.hideSuggestionAfter);
            mActionRowContainer.setVisibility(View.GONE);
            this.mSuggestionsStripHackyContainer.setVisibility(View.VISIBLE);
            mActionRowContainer.postDelayed(this.hideSuggestionAfter, 2000);
        }
    }
}
