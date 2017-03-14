package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.ybq.android.spinkit.SpinKitView;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.slash.RDetail;
import io.separ.neural.inputmethod.slash.RSearchItem;
import io.separ.neural.inputmethod.slash.RServiceItem;

import static io.separ.neural.inputmethod.Utils.ColorUtils.colorProfile;
import static io.separ.neural.inputmethod.slash.RSearchItem.GENERIC_MESSAGE_TYPE;
import static io.separ.neural.inputmethod.slash.RSearchItem.LOADING_TYPE;
import static io.separ.neural.inputmethod.slash.RSearchItem.MEDIA_TYPE;
import static io.separ.neural.inputmethod.slash.RSearchItem.PERMISSION_REQUIRED_TYPE;

/**
 * Created by sepehr on 3/2/17.
 */

public class SearchItemArrayAdapter extends ArrayAdapter<RSearchItem, SearchItemArrayAdapter.SearchItemViewHolder>{
    public static final int RESULT_TYPE_CONNECT_TO_USE = 5;
    public static final int RESULT_TYPE_DEFAULT = 0;
    public static final int RESULT_TYPE_GENERIC_MESSAGE = 6;
    public static final int RESULT_TYPE_IMAGE = 1;
    public static final int RESULT_TYPE_IMAGE_NO_BORDER = 2;
    public static final int RESULT_TYPE_LOADING = -1;
    public static final int RESULT_TYPE_PERMISSION_REQUIRED = 7;
    private IOnClickListener mClickListener;
    private float mImageHeight;
    private Runnable mLoadNextPage;
    private RServiceItem mServiceItem;

    public interface IOnClickListener {
        void onClick(boolean z, boolean z2, boolean z3, int i);
    }

    public static class SearchItemViewHolder extends RecyclerView.ViewHolder implements Runnable {
        private final SpinKitView loading;
        TextView bottom;
        Button connectButton;
        TextView header;
        SimpleDraweeView imageView;
        public boolean isBottomEmpty;
        private IOnClickListener mClickListener;
        private int mPosition;
        ImageButton preview;
        TextView subheader;
        View titleContainer;

        /* renamed from: co.touchlab.inputmethod.latin.monkey.ui.adapter.SearchItemArrayAdapter.SearchItemViewHolder.1 */
        class C04441 implements View.OnClickListener {
            C04441() {
            }

            public void onClick(View v) {
                if (SearchItemViewHolder.this.mClickListener != null) {
                    SearchItemViewHolder.this.mClickListener.onClick(false, true, false, SearchItemViewHolder.this.mPosition);
                }
            }
        }

        /* renamed from: co.touchlab.inputmethod.latin.monkey.ui.adapter.SearchItemArrayAdapter.SearchItemViewHolder.2 */
        class C04452 implements View.OnClickListener {
            C04452() {
            }

            public void onClick(View v) {
                if (SearchItemViewHolder.this.mClickListener != null) {
                    SearchItemViewHolder.this.mClickListener.onClick(false, false, true, SearchItemViewHolder.this.mPosition);
                }
            }
        }

        /* renamed from: co.touchlab.inputmethod.latin.monkey.ui.adapter.SearchItemArrayAdapter.SearchItemViewHolder.3 */
        class C04463 implements View.OnClickListener {
            C04463() {
            }

            public void onClick(View v) {
                if (SearchItemViewHolder.this.mClickListener != null) {
                    SearchItemViewHolder.this.mClickListener.onClick(true, false, false, SearchItemViewHolder.this.mPosition);
                }
            }
        }

        public SearchItemViewHolder(View view, IOnClickListener clickListener) {
            super(view);
            this.mClickListener = clickListener;
            this.imageView = (SimpleDraweeView) view.findViewById(R.id.item_image);
            this.header = (TextView) view.findViewById(R.id.header);
            this.subheader = (TextView) view.findViewById(R.id.subheader);
            this.titleContainer = view.findViewById(R.id.title_container);
            this.bottom = (TextView) view.findViewById(R.id.item_bottom);
            this.preview = (ImageButton) view.findViewById(R.id.preview);
            this.connectButton = (Button) view.findViewById(R.id.connect_button);
            if (this.connectButton != null) {
                this.connectButton.setOnClickListener(new C04452());
            }
            this.loading = (SpinKitView) view.findViewById(R.id.service_loading_container);
            if (this.preview != null) {
                this.preview.setOnClickListener(new C04441());
            }
            view.setOnClickListener(new C04463());
        }

        public void updatePosition(RSearchItem item, int position) {
            this.mPosition = position;
            if (this.preview == null) {
                return;
            }
            if (TextUtils.isEmpty(item.getPreviewUrl())) {
                this.preview.setVisibility(View.GONE);
            } else {
                this.preview.setVisibility(View.VISIBLE);
            }
        }

        public void run() {
            if (this.header.getLineCount() < SearchItemArrayAdapter.RESULT_TYPE_IMAGE_NO_BORDER) {
                return;
            }
            if (this.isBottomEmpty) {
                this.subheader.setMaxLines(3);
            } else {
                this.subheader.setMaxLines(SearchItemArrayAdapter.RESULT_TYPE_IMAGE_NO_BORDER);
            }
        }
    }

    public SearchItemArrayAdapter(Context context) {
        super(context);
        this.mImageHeight = getContext().getResources().getDimension(R.dimen.icon_height);
    }

    public void setOnSearchItemClickListener(IOnClickListener clickListener) {
        this.mClickListener = clickListener;
    }

    public void setImageHeightBig() {
        this.mImageHeight = getContext().getResources().getDimension(R.dimen.icon_height_big);
    }

    public void setImageHeightSmall() {
        this.mImageHeight = getContext().getResources().getDimension(R.dimen.icon_height);
    }

    public void setServiceItem(RServiceItem serviceItem) {
        this.mServiceItem = serviceItem;
    }

    public SearchItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        int layoutId;
        switch (viewType) {
            case RESULT_TYPE_LOADING /*-1*/:
                layoutId = R.layout.item_loading;
                break;
            case RESULT_TYPE_IMAGE /*1*/:
                layoutId = R.layout.item_search_image;
                break;
            case RESULT_TYPE_IMAGE_NO_BORDER /*2*/:
                layoutId = R.layout.item_search_image_no_border;
                break;
            case RESULT_TYPE_PERMISSION_REQUIRED /*7*/:
                layoutId = R.layout.item_connect;
                break;
            case RESULT_TYPE_GENERIC_MESSAGE /*6*/:
                layoutId = R.layout.item_generic_message;
                break;
            default:
                layoutId = R.layout.item_search;
                break;
        }
        return new SearchItemViewHolder(getLayoutInflater().inflate(layoutId, viewGroup, false), this.mClickListener);
    }

    public void onBindViewHolder(SearchItemViewHolder vh, int pos) {
        int itemViewType = getItemViewType(pos);
        RSearchItem item = getItem(pos);
        vh.updatePosition(item, pos);
        boolean titleEmpty = TextUtils.isEmpty(item.getTitle());
        String subheader = getSubheaderText(item);
        SpannableStringBuilder bottom = getBottomText(item, vh.bottom != null ? vh.bottom.getLineHeight() : RESULT_TYPE_DEFAULT);
        boolean subtitleEmpty = TextUtils.isEmpty(subheader);
        boolean bottomEmpty = TextUtils.isEmpty(bottom);
        switch (itemViewType) {
            case RESULT_TYPE_DEFAULT /*0*/:
                if (titleEmpty && subtitleEmpty) {
                    vh.titleContainer.setVisibility(View.GONE);
                } else {
                    vh.titleContainer.setVisibility(View.VISIBLE);
                    vh.header.setText(item.getTitle());
                    if (subtitleEmpty) {
                        vh.subheader.setVisibility(View.GONE);
                    } else {
                        vh.subheader.setVisibility(View.VISIBLE);
                        vh.subheader.setText(subheader);
                        vh.subheader.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    }
                    if (bottomEmpty) {
                        vh.bottom.setVisibility(View.GONE);
                    } else {
                        vh.bottom.setVisibility(View.VISIBLE);
                        vh.bottom.setText(bottom);
                        vh.bottom.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    }
                }
                resizeTextViews(vh, bottomEmpty);
                if (item.getImage() == null) {
                    vh.imageView.setVisibility(View.GONE);
                    break;
                }
                vh.imageView.setVisibility(View.VISIBLE);
                ImageUtils.showSearchItemImage(vh.imageView, item);
                break;
            case RESULT_TYPE_IMAGE /*1*/:
            case RESULT_TYPE_IMAGE_NO_BORDER /*2*/:
                vh.imageView.setVisibility(View.VISIBLE);
                /*float itemWidth = this.mImageHeight * item.getImage().getAspectRatio();
                ViewGroup.LayoutParams layoutParams = vh.imageView.getLayoutParams();
                layoutParams.width = Math.round(itemWidth);
                layoutParams.height = (int) this.mImageHeight;
                vh.imageView.setLayoutParams(layoutParams);
                vh.imageView.requestLayout();*/
                ImageUtils.showSearchItemImage(vh.imageView, item);
                break;
            case RESULT_TYPE_GENERIC_MESSAGE /*6*/:
                vh.header.setText(item.getTitle());
                break;
            case RESULT_TYPE_LOADING:
                vh.loading.setVisibility(View.VISIBLE);
                vh.titleContainer.setVisibility(View.VISIBLE);
                break;
            case RESULT_TYPE_PERMISSION_REQUIRED /*7*/:
                updateActionRequiredItem(vh, RServiceItem.serviceItemHashMap.get(item.getService()), item.getOutput(), item.getTitle(), getContext().getString(R.string.allow_permission));
                break;
        }
        if(vh.header != null)
            vh.header.setTextColor(colorProfile.getText());
        if(vh.subheader != null)
            vh.subheader.setTextColor(colorProfile.getText());
        if(vh.bottom != null)
            vh.bottom.setTextColor(colorProfile.getText());
        if(vh.titleContainer != null)
            vh.titleContainer.setBackgroundColor(colorProfile.getPrimary());
        if(vh.preview != null)
            vh.preview.setColorFilter(colorProfile.getIcon());
        if (pos > getItemCount() - 2 && this.mLoadNextPage != null) {
            this.mLoadNextPage.run();
        }
    }

    private void updateActionRequiredItem(SearchItemViewHolder vh, RServiceItem serviceItem, String descriptionText, String highlightWord, String buttonText) {
        SpannableStringBuilder str = new SpannableStringBuilder(descriptionText);
        if (descriptionText != null) {
            int startPosition = descriptionText.indexOf(highlightWord);
            if (startPosition > 0) {
                str.setSpan(new StyleSpan(RESULT_TYPE_IMAGE), startPosition, highlightWord.length() + startPosition, 33);
            }
        }
        vh.header.setText(str);
        vh.connectButton.setText(buttonText);
        if (serviceItem != null) {
            ImageUtils.showColoredImage(vh.imageView, serviceItem);
        }
        ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
        layoutParams.height = (int) this.mImageHeight;
        vh.itemView.setLayoutParams(layoutParams);
        vh.itemView.requestLayout();
    }

    public void setPageLoadingListener(Runnable listener) {
        this.mLoadNextPage = listener;
    }

    private void resizeTextViews(SearchItemViewHolder vh, boolean isBottomEmpty) {
        vh.header.setMaxLines(RESULT_TYPE_IMAGE_NO_BORDER);
        if (isBottomEmpty) {
            vh.subheader.setMaxLines(4);
        } else {
            vh.subheader.setMaxLines(3);
        }
        vh.isBottomEmpty = isBottomEmpty;
        vh.header.post(vh);
    }

    private String getSubheaderText(RSearchItem item) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(item.getSubtitle());
        RDetail detail = item.getDetail();
        if (detail == null) {
            return stringBuilder.toString();
        }
        if (!TextUtils.isEmpty(detail.getViews())) {
            stringBuilder.append("\n");
            stringBuilder.append("\u25b6\ufe0e");
            stringBuilder.append(detail.getViews());
        }
        if (!TextUtils.isEmpty(detail.getAlbum())) {
            stringBuilder.append("\n");
            stringBuilder.append(detail.getAlbum());
        }
        return stringBuilder.toString();
    }

    private SpannableStringBuilder getBottomText(RSearchItem item, int lineHeight) {
        RDetail detail = item.getDetail();
        if (detail == null) {
            return new SpannableStringBuilder();
        }
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        boolean somethingAppended = false;
        if (!TextUtils.isEmpty(detail.getDuration())) {
            stringBuilder.append(detail.getDuration());
            somethingAppended = true;
        }
        if (!TextUtils.isEmpty(detail.getDistance())) {
            stringBuilder.append(detail.getDistance());
            somethingAppended = true;
        }
        if (!TextUtils.isEmpty(detail.getPrice())) {
            if (somethingAppended) {
                stringBuilder.append("   ");
            }
            stringBuilder.append(detail.getPrice());
            if (somethingAppended) {
                stringBuilder.setSpan(new ForegroundColorSpan(-8816263), stringBuilder.length() - detail.getPrice().length(), stringBuilder.length(), 18);
            }
            somethingAppended = true;
        }
        if (!(TextUtils.isEmpty(detail.getDate()) || somethingAppended)) {
            stringBuilder.append(detail.getDate());
            somethingAppended = true;
        }
        if (TextUtils.isEmpty(detail.getRating()) || somethingAppended) {
            return stringBuilder;
        }
        stringBuilder.append("\u25b2");
        stringBuilder.append(detail.getRating());
        return stringBuilder;
    }

    public int getItemViewType(int r7) {
        RSearchItem item = getItem(r7);
        String displayType = item.getDisplayType();
        if(displayType.equals(GENERIC_MESSAGE_TYPE))
            return RESULT_TYPE_GENERIC_MESSAGE;
        else if(displayType.equals(MEDIA_TYPE))
            return RESULT_TYPE_IMAGE;
        else if(displayType.equals(LOADING_TYPE))
            return RESULT_TYPE_LOADING;
        else if(displayType.equals(PERMISSION_REQUIRED_TYPE))
            return RESULT_TYPE_PERMISSION_REQUIRED;
        else
            return RESULT_TYPE_DEFAULT;
    }
}
