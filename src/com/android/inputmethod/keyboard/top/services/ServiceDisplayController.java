package com.android.inputmethod.keyboard.top.services;

/**
 * Created by sepehr on 3/2/17.
 */

public class ServiceDisplayController {
    /*private final ServiceResultsView mServiceResultsView;

    public ServiceDisplayController(View parent) {
        this.mServiceResultsView = (ServiceResultsView) parent.findViewById(R.id.suggestion_source_results);
        this.mServiceResultsView.setVisibility(View.GONE);
    }

    public void drop() {
        this.mServiceResultsView.drop();
    }

    public int getHeight() {
        return (mServiceResultsView.getVisibility() == View.VISIBLE) ? mServiceResultsView.getHeight() : 0;
    }

    public void hideAll() {
        updateBarVisibility();
        hideResults();
    }

    private void hideResults() {
        this.mServiceResultsView.setVisibility(View.GONE);
        this.mServiceResultsView.reset();
    }

    public void showRetryErrorMessage(boolean network) {
        this.mServiceResultsView.showRetryErrorMessage(network);
    }

    public void setVisualState(VisualSate state) {
        this.mServiceResultsView.setVisualState(state);
    }

    public void runSearch(String slash, String searchString) {
        this.mServiceResultsView.runSearch(slash, searchString, null);//8==GONE
    }

    public void setSearchItems(String slash, List<RSearchItem> items, String authorizedStatus) {
        setVisualState(VisualSate.Results);
        this.mServiceResultsView.setSearchItems(slash, items, authorizedStatus);
    }

    public void updateBarVisibility() {
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
    }*/
}
