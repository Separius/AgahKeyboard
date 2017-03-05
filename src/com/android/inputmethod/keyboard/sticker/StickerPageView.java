package com.android.inputmethod.keyboard.sticker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.IOException;
import java.io.InputStream;

import io.separ.neural.inputmethod.indic.R;

import static io.separ.neural.inputmethod.Utils.ColorUtils.colorProfile;

/**
 * Created by sepehr on 3/5/17.
 */
public class StickerPageView extends FrameLayout {
    private GridView grid;

    public StickerPageView(Context context) {
        this(context, null);
    }

    public StickerPageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.sticker_grid_layout, this, true);
        grid = (GridView) view.findViewById(R.id.sticker_grid);
        //TODO change these numbers and also in grid_layout
        grid.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.emoji_drawer_size)*2 + 2 * getResources().getDimensionPixelSize(R.dimen.emoji_drawer_item_padding));
        grid.setBackgroundColor(colorProfile.getPrimary());
    }

    public void setModel(StickerPageModel model) {
        grid.setAdapter(new StickerGridAdapter(getContext(), model));
    }

    private static class StickerGridAdapter extends BaseAdapter {

        protected final Context                context;
        private   final int                    emojiSize;
        private final String[] modelStickers;
        private final String baseAddress;
        //private EmojiPageView.EmojiSelectionListener listener;

        public StickerGridAdapter(Context context, StickerPageModel model) {
            this.context   = context;
            this.emojiSize = (int) context.getResources().getDimension(R.dimen.emoji_drawer_size)*2;
            modelStickers = model.getPack();
            baseAddress = model.getName();
        }

        /*public void setListener(EmojiPageView.EmojiSelectionListener listener){
            this.listener = listener;
        }*/

        @Override public int getCount() {
            return modelStickers.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            /*if(listener != null)
                listener.onEmojiSelected(modelEmojis[position]);*/
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(emojiSize+2*5, emojiSize+2*5));
                //imageView.setScaleType(ImageView.ScaleType.CENTER);
                imageView.setPadding(5, 5, 5, 5);
            } else {
                imageView = (ImageView) convertView;
            }
            Glide.with(context)
                    .load(Uri.parse("file:///android_asset/stickers/"+baseAddress+"/"+modelStickers[position]))
                    .fitCenter()
                    .into(imageView);
            return imageView;
        }
    }
}
