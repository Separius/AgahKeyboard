package com.android.inputmethod.keyboard.top.services;

/**
 * Created by sepehr on 3/2/17.
 */

import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.suggestions.SuggestionStripView;

public class ServiceDisplayController {
    private final ServiceBarView mServiceBarView;
    private final ServiceResultsView mServiceResultsView;

    public ServiceDisplayController(View parent) {
        this.mServiceResultsView = (ServiceResultsView) parent.findViewById(R.id.suggestion_source_results);
        this.mServiceBarView = (ServiceBarView) parent.findViewById(R.id.services_bar);
        this.mServiceResultsView.setVisibility(View.GONE);
    }

    public void drop() {
        this.mServiceBarView.drop();
        this.mServiceResultsView.drop();
    }

    public int getHeight() {
        boolean somethingsShowing;
        if (this.mSuggestionsStripView.getVisibility() == 0 || this.mServiceResultsView.getVisibility() == 0 || this.mServiceBarView.getVisibility() == 0) {
            somethingsShowing = true;
        } else {
            somethingsShowing = false;
        }
        if (somethingsShowing) {
            return this.holderLayout.getHeight();
        }
        return 0;
    }

    public void hideAll() {
        updateBarVisibility();
        hideResults();
    }

    private void hideResults() {
        this.mServiceResultsView.setVisibility(8);
        this.mServiceResultsView.reset();
    }

    public void showRetryErrorMessage(boolean network) {
        this.mServiceResultsView.showRetryErrorMessage(network);
    }

    public void setVisualState(VisualSate state) {
        this.mServiceResultsView.setVisualState(state);
    }

    public void runSearch(String slash, String searchString) {
        this.mServiceBarView.setVisibility(8);//8==GONE
        this.mServiceResultsView.runSearch(slash, searchString, null);
        if (this.mSuggestionsStripHackyContainer.getVisibility() != 8) {
            this.mSuggestionsStripHackyContainer.setVisibility(8);
        }
    }

    public void setSearchItems(String slash, List<RSearchItem> items, String authorizedStatus) {
        setVisualState(VisualSate.Results);
        this.mServiceBarView.setVisibility(8);
        this.mServiceResultsView.setSearchItems(slash, items, authorizedStatus);
    }

    public void updateBarVisibility() {
        this.mServiceBarView.setVisibility(0);
        this.mSuggestionsStripHackyContainer.setVisibility(8);
        return;
    }

    public void showSuggestions() {
        if (this.mSuggestionsStripView.getVisibility() == 0) {
            if (this.mServiceResultsView.getVisibility() == 0) {
                this.mSuggestionsStripHackyContainer.setVisibility(8);
                this.mServiceBarView.setVisibility(8);
                return;
            }
            this.mServiceBarView.removeCallbacks(this.hideSuggestionAfter);
            this.mServiceBarView.setVisibility(8);
            this.mSuggestionsStripHackyContainer.setVisibility(0);
            this.mServiceBarView.postDelayed(this.hideSuggestionAfter, 2000);
        }
    }
}
