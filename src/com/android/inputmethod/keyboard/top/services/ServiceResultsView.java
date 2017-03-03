package com.android.inputmethod.keyboard.top.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.slash.EventBusExt;
import io.separ.neural.inputmethod.slash.NeuralApplication;
import io.separ.neural.inputmethod.slash.RCategory;
import io.separ.neural.inputmethod.slash.RSearchItem;
import io.separ.neural.inputmethod.slash.RServiceItem;
import io.separ.neural.inputmethod.slash.ServiceQueryContactsTask;
import io.separ.neural.inputmethod.slash.ServiceQuerySearchTask;
import io.separ.neural.inputmethod.slash.ServiceRequestManager;
import io.separ.neural.inputmethod.slash.TaskQueueHelper;

/**
 * Created by sepehr on 3/2/17.
 */

public class ServiceResultsView extends LinearLayout {
    private CategoriesRecyclerView mCategoriesList;
    private VisualSate mPreviousState;
    private ResultsRecyclerView mRecycler;
    private View mSearchContainer;
    private TextView mSearchMirror;
    private ImageView mSearchPlaceholder;
    private TextView mSourceError;
    private SimpleDraweeView mSourceImageView;
    private ProgressBar mSourceProgress;
    private String currentSlash;
    private String currentContext;

    /*class C04621 implements OnClickListener {
        C04621() {
        }

        public void onClick(View v) {
            ServiceRequestManager.getInstance().repostLastRequest();
        }
    }*/

    class C04632 implements SearchItemArrayAdapter.IOnClickListener {
        C04632() {
        }

        public void onClick(boolean parentClicked, boolean previewClicked, boolean connectClicked, int position) {
            if (ServiceResultsView.this.mRecycler.getAdapter().getItemCount() > position) {
                if (parentClicked) {
                    ServiceResultsView.this.onItemClicked(position);
                }
                if (previewClicked) {
                    ServiceResultsView.this.onPreviewClicked(position);
                }
            }
        }
    }

    class C04643 implements CategoriesArrayAdapter.IOnClickListener {
        C04643() {
        }

        public void onClick(int position) {
            if (position < ServiceResultsView.this.mCategoriesList.getAdapter().getItemCount()) {
                RCategory category = ServiceResultsView.this.mCategoriesList.getAdapter().getItem(position);
                ServiceResultsView.this.runSearch(category.getAction(), category.getType());
            }
        }
    }

    class C04654 implements Runnable {
        final /* synthetic */ String val$slash;

        C04654(String str) {
            this.val$slash = str;
        }

        public void run() {
            ServiceResultsView.this.setServiceImage(this.val$slash);
            ServiceResultsView.this.mSourceImageView.setRotationY(-90.0f);
            ServiceResultsView.this.mSourceImageView.animate().rotationY(0.0f).setDuration(75).scaleX(SimpleItemTouchHelperCallback.ALPHA_FULL).scaleY(SimpleItemTouchHelperCallback.ALPHA_FULL);
        }
    }

    static /* synthetic */ class C04687 {
        static final /* synthetic */ int[] f932x310c8f71;

        static {
            f932x310c8f71 = new int[VisualSate.values().length];
            try {
                f932x310c8f71[VisualSate.Loading.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f932x310c8f71[VisualSate.ServiceError.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                f932x310c8f71[VisualSate.Hide.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                f932x310c8f71[VisualSate.GeneralError.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                f932x310c8f71[VisualSate.Results.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public enum VisualSate {
        Hide,
        Loading,
        ServiceError,
        GeneralError,
        Results;

        private String message;

        VisualSate() {}

        public VisualSate setMessage(String msg) {
            this.message = msg;
            return this;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public ServiceResultsView(Context context) {
        this(context, null);
    }

    public ServiceResultsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ServiceResultsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public ServiceResultsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    class C04621 implements OnClickListener {
        C04621() {
        }

        public void onClick(View v) {
            ServiceRequestManager.getInstance().repostLastRequest();
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.source_results_strip, this);
        animate().setDuration(75);
        this.mSourceImageView = (SimpleDraweeView) findViewById(R.id.source_image);
        this.mSourceImageView.setCameraDistance(5000.0f);
        this.mSourceImageView.animate().setInterpolator(new LinearInterpolator()).setDuration(150);
        this.mSourceError = (TextView) findViewById(R.id.source_error);
        this.mSourceError.setOnClickListener(new C04621());
        this.mSourceError.setClickable(false);
        //this.mSourceError.setTextColor(ColorUtils.colorProfile.getTextColor());
        this.mSourceProgress = (ProgressBar) findViewById(R.id.source_progress);
        this.mRecycler = (ResultsRecyclerView) findViewById(R.id.source_recycler);
        this.mRecycler.getAdapter().setOnSearchItemClickListener(new C04632());
        this.mCategoriesList = (CategoriesRecyclerView) findViewById(R.id.categories);
        this.mCategoriesList.getAdapter().setOnCategoryClickListener(new C04643());
        this.mSearchContainer = findViewById(R.id.search_container);
        this.mSearchPlaceholder = (ImageView) findViewById(R.id.search_placeholder);
        this.mSearchMirror = (TextView) findViewById(R.id.slash_search_mirror);
        ImageUtils.updateTheme(context);
        if (ImageUtils.isLightTheme()) {
            this.mSearchPlaceholder.setColorFilter(-5524809);
        }
        setVisualState(TaskQueueHelper.hasTasksOfType(NeuralApplication.getNetworkTaskQueue(), ServiceQuerySearchTask.class, ServiceQueryContactsTask.class) ? VisualSate.Loading : VisualSate.Hide);
        currentContext = new String();
        currentSlash = new String();
    }

    public void drop() {
        this.mRecycler.drop();
        this.mCategoriesList.drop();
    }

    public void increasePaddingBetweenResultsAndCategory() {
        LayoutParams params = (LayoutParams) this.mCategoriesList.getLayoutParams();
        params.setMargins(0, (int) (5.0f * getResources().getDisplayMetrics().density), 0, 0);
        this.mCategoriesList.setLayoutParams(params);
    }

    public ResultsRecyclerView getRecycler() {
        return this.mRecycler;
    }

    public void startSearch(String slash, String context){
        setVisibility(VISIBLE);
        setService(slash);
        currentSlash = slash;
        currentContext = context;
    }

    public void runSearch(String searchString, String type) {
        String action = null;
        int categoryIndex = this.mCategoriesList.getAdapter().getSelectedCategoryIndex();
        boolean useCaching = false;
        if ("act".equals(type)) {
            if (categoryIndex == 0) {
                action = "prepopulate";
                useCaching = true;
            } else {
                action = this.mCategoriesList.getAdapter().getSelectedCategoryAction();
            }
            setSearchMirror("");
        } else if ("aac".equals(type)) {
            if (categoryIndex == 0) {
                action = "prepopulate";
                useCaching = true;
            } else {
                action = this.mCategoriesList.getAdapter().getSelectedCategoryAction();
            }
            setSearchMirror("");
        } else if ("sug".equals(type)) {
            if (categoryIndex == 0) {
                action = "prepopulate";
                useCaching = true;
            }
            setSearchMirror("");
        } else {
            if (TextUtils.isEmpty(searchString)) {
                if(TextUtils.isEmpty(currentContext)) {
                    action = "prepopulate";
                    useCaching = true;
                    this.mCategoriesList.getAdapter().setSelectedItem(0);
                }else{
                    searchString = currentContext;
                    this.mCategoriesList.getAdapter().setSelectedItem(-1);
                }
            } else {
                this.mCategoriesList.getAdapter().setSelectedItem(-1);
            }
            setSearchMirror(searchString);
        }
        updateCategoryVisibility();
        ServiceRequestManager.getInstance().cancelLastRequest();
        ServiceRequestManager.getInstance().postRequest(currentSlash, searchString, action, useCaching);
    }

    public void clear() {
        if (this.mRecycler != null) {
            this.mRecycler.getLayoutManager().scrollToPosition(0);
        }
        if (this.mRecycler != null) {
            this.mRecycler.getAdapter().clear();
        }
    }

    public void reset() {
        clear();
        this.mSourceImageView.setImageDrawable(null);
        this.mSourceImageView.setTag(null);
        setVisualState(VisualSate.Hide);
    }

    public void setSearchMirror(String s) {
        this.mSearchMirror.setText(s);
    }

    public void setSearchMirrorHint(String slash) {
        if (this.mSearchMirror.getTag() == null || !slash.equals((String) this.mSearchMirror.getTag())) {
            RServiceItem item = new RServiceItem(); //TODO make a constructor for this
            item.setSlash(slash);
            item.setMySlash(false);
            if (item != null) {
                this.mSearchMirror.setHint(item.getSearchPlaceholder());
            }
            this.mSearchMirror.setTag(slash);
        }
    }

    public void setService(String slash) {
        boolean serviceChanged = this.mRecycler.setService(slash);
        this.mCategoriesList.setService(slash);
        if (serviceChanged) {
            setServiceImageWithAnimation(slash);
            setSearchMirrorHint(slash);
            this.mSearchContainer.setVisibility(VISIBLE);
            updateCategoryVisibility();
        }
    }

    public void setSearchItems(String slash, List<RSearchItem> items, String authorizedStatus) {
        setVisibility(VISIBLE);
        setService(slash);
        if (getVisibility() == VISIBLE) {
            boolean unauthPreview = this.mRecycler.trySetUnauthPreviewItems(authorizedStatus, true);
            if ((items != null && !items.isEmpty()) || this.mRecycler.isShowingUnauthPreviewItems()) {
                this.mRecycler.setItems(items);
                setVisualState(VisualSate.Results);
            } else if (!unauthPreview && !RServiceItem.PHOTOS.equals(slash)) {
                this.mRecycler.setEmptyItem();
            }
        }
    }

    private void updateCategoryVisibility() {
        if (this.mCategoriesList.isEmpty()) {
            this.mCategoriesList.setVisibility(GONE);
        } else {
            this.mCategoriesList.setVisibility(VISIBLE);
        }
    }

    private void setServiceImageWithAnimation(String slash) {
        if (this.mSourceImageView.getTag() == null || !slash.equals(this.mSourceImageView.getTag())) {
            if (isViewShown()) {
                this.mSourceImageView.animate().rotationY(90.0f).scaleX(0.8f).scaleY(0.8f).setDuration(75).withEndAction(new C04654(slash));
            } else {
                setServiceImage(slash);
            }
            this.mSourceImageView.setTag(slash);
        }
    }

    private void onItemClicked(int position) {
        RSearchItem searchItem = this.mRecycler.getAdapter().getItem(position);
        if (!RSearchItem.LOADING_TYPE.equals(searchItem.getDisplayType()) && !RSearchItem.CONNECT_TO_USE_TYPE.equals(searchItem.getDisplayType()) && !RSearchItem.GENERIC_MESSAGE_TYPE.equals(searchItem.getDisplayType()) && !RSearchItem.PERMISSION_REQUIRED_TYPE.equals(searchItem.getDisplayType()))
            onNormalItemClick(searchItem, position);
    }

    private void onNormalItemClick(RSearchItem searchItem, int position) {
        String source = searchItem.getCorrectService();
        EventBusExt.getDefault().post(new SearchItemSelectedEvent(source, searchItem, Integer.valueOf(position)));
        reset();
    }

    private void onPreviewClicked(int position) {
        ResultsRecyclerView.openPreview(getContext(), (String)this.mRecycler.getAdapter().getItem(position).getPreviewUrl());
    }

    private boolean isViewShown() {
        return getVisibility() == VISIBLE;
    }

    private void setServiceImage(String slash) {
        RServiceItem item = new RServiceItem();
        item.setSlash(slash);
        item.setMySlash(false);
        if (item != null) {
            ImageUtils.showColoredImage(this.mSourceImageView, item);//TODO make it work
        }
    }

    public void showRetryErrorMessage(boolean network) {
        setVisualState(VisualSate.ServiceError.setMessage(getResources().getString(network ? R.string.service_result_network_problem : R.string.service_result_problem)));
        this.mSourceError.setClickable(true);
    }

    public void setVisualState(VisualSate state) {
        switch (C04687.f932x310c8f71[state.ordinal()]) {
            case 1:
                this.mRecycler.setVisibility(VISIBLE);
                updateCategoryVisibility();
                this.mSourceImageView.setVisibility(VISIBLE);
                setServiceImageWithAnimation(state.getMessage());
                this.mSourceError.setVisibility(GONE);
                this.mSourceError.setClickable(false);
                if (this.mPreviousState == VisualSate.Loading) {
                    return;
                }
                if (this.mRecycler.serviceChanged()) {
                    this.mRecycler.setLoadingItems();
                    this.mPreviousState = VisualSate.Loading;
                    this.mSourceProgress.setVisibility(GONE);
                    return;
                }
                this.mSourceProgress.setVisibility(VISIBLE);
                break;
            case 2:
                this.mRecycler.setVisibility(GONE);
                this.mSourceImageView.setVisibility(VISIBLE);
                this.mSourceProgress.setVisibility(GONE);
                this.mSourceError.setText(state.getMessage());
                this.mSourceError.setVisibility(VISIBLE);
                this.mSourceError.setClickable(false);
                this.mPreviousState = VisualSate.ServiceError;
                this.mRecycler.setPreviousSlash("");
                break;
            case 3:
                this.mPreviousState = VisualSate.Hide;
                this.mRecycler.setPreviousSlash("");
                this.mCategoriesList.getAdapter().setSelectedItem(0);
                break;
            case 4:
                this.mSourceError.setText(state.getMessage());
                this.mRecycler.setVisibility(GONE);
                this.mSourceImageView.setVisibility(GONE);
                this.mSourceProgress.setVisibility(GONE);
                this.mSourceError.setVisibility(VISIBLE);
                this.mSourceError.setClickable(false);
                this.mPreviousState = VisualSate.GeneralError;
                this.mRecycler.setPreviousSlash("");
                break;
            case 5:
                this.mRecycler.setVisibility(VISIBLE);
                updateCategoryVisibility();
                this.mSourceImageView.setVisibility(VISIBLE);
                this.mSourceProgress.setVisibility(GONE);
                this.mSourceError.setVisibility(GONE);
                this.mPreviousState = VisualSate.Results;
                this.mRecycler.setPreviousSlash(this.mRecycler.getCurrentSlash());
                break;
            default:
                throw new RuntimeException("VisualSate " + state + " is not supported");
        }
    }
}