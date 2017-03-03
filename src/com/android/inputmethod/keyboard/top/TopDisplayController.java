package com.android.inputmethod.keyboard.top;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.inputmethod.keyboard.top.actionrow.ActionRowView;
import com.android.inputmethod.keyboard.top.services.ServiceResultsView;

import java.util.List;
import java.util.Random;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.suggestions.SuggestionStripView;
import io.separ.neural.inputmethod.slash.RSearchItem;

import static android.view.View.GONE;

/**
 * Created by sepehr on 3/2/17.
 */

//TODO when latin ime stops us, we have to cancel old queries

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

    public void setVisualState(ServiceResultsView.VisualSate visualState) {
        mServiceResultsView.setVisualState(visualState);
    }

    public void showRetryErrorMessage(boolean network) {
        this.mServiceResultsView.showRetryErrorMessage(network);
    }

    public void setSearchItems(String slash, List<RSearchItem> items, String authorizedStatus) {
        setVisualState(ServiceResultsView.VisualSate.Results);
        this.mActionRowContainer.setVisibility(GONE);
        this.mServiceResultsView.setSearchItems(slash, items, authorizedStatus);
    }

    public void runSearch(String query){
        mServiceResultsView.runSearch(query, null);
    }

    public void runSearch(String serviceId, String context) {
        this.mActionRowContainer.setVisibility(GONE);
        this.mServiceResultsView.startSearch(serviceId, context);
        if (this.mSuggestionsStripHackyContainer.getVisibility() != GONE) {
            this.mSuggestionsStripHackyContainer.setVisibility(GONE);
        }
    }

    class C04611 implements Runnable {
        C04611() {
        }
        public void run() {
            mActionRowContainer.setVisibility(View.VISIBLE);
            mSuggestionsStripHackyContainer.setVisibility(GONE);
        }
    }

    public void updateBarVisibility() {
        mActionRowContainer.setVisibility(View.VISIBLE);
        mSuggestionsStripHackyContainer.setVisibility(GONE);
    }

    public void drop() {
        this.mServiceResultsView.drop();
    }

    public void hideAll() {
        mServiceResultsView.setVisibility(GONE);
        updateBarVisibility();
        mServiceResultsView.reset();
    }

    public TopDisplayController(View parent){
        hideSuggestionAfter = new C04611();
        mActionRowContainer = parent.findViewById(R.id.action_row_container);
        holderLayout = (LinearLayout) parent.findViewById(R.id.keyboard_top_area);
        mActionRowView = (ActionRowView) parent.findViewById(R.id.action_row);
        mSuggestionsStripView = (SuggestionStripView) parent.findViewById(R.id.suggestion_strip_view);
        mSuggestionsStripHackyContainer = parent.findViewById(R.id.suggestion_strip_hacky_container);
        mSuggestionsStripHackyContainer.setVisibility(GONE);
        mServiceResultsView = (ServiceResultsView) parent.findViewById(R.id.suggestion_source_results);
        mServiceResultsView.setVisibility(GONE);
    }

    public void showSuggestions() {
        if (this.mSuggestionsStripView.getVisibility() == View.VISIBLE) {
            if (this.mServiceResultsView.getVisibility() == View.VISIBLE) {
                this.mSuggestionsStripHackyContainer.setVisibility(GONE);
                return;
            }
            mActionRowContainer.removeCallbacks(this.hideSuggestionAfter);
            mActionRowContainer.setVisibility(GONE);
            this.mSuggestionsStripHackyContainer.setVisibility(View.VISIBLE);
            mActionRowContainer.postDelayed(this.hideSuggestionAfter, 2000);
        }
    }
}
