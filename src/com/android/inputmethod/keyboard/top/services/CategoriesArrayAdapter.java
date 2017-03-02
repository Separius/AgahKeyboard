package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by sepehr on 3/2/17.
 */
public class CategoriesArrayAdapter extends ArrayAdapter<RCategory, CategoryViewHolder> {
    private IOnClickListener mClickListener;
    private int mSelectedItem;

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        private IOnClickListener mClickListener;
        private int mPosition;
        private TextView name;

        /* renamed from: co.touchlab.inputmethod.latin.monkey.ui.adapter.CategoriesArrayAdapter.CategoryViewHolder.1 */
        class C04431 implements View.OnClickListener {
            final /* synthetic */ CategoriesArrayAdapter val$this$0;

            C04431(CategoriesArrayAdapter categoriesArrayAdapter) {
                this.val$this$0 = categoriesArrayAdapter;
            }

            public void onClick(View v) {
                if (CategoryViewHolder.this.mClickListener != null) {
                    CategoriesArrayAdapter.this.mSelectedItem = CategoryViewHolder.this.mPosition;
                    CategoriesArrayAdapter.this.notifyDataSetChanged();
                    CategoryViewHolder.this.mClickListener.onClick(CategoryViewHolder.this.mPosition);
                }
            }
        }

        public CategoryViewHolder(View view, IOnClickListener clickListener) {
            super(view);
            this.mClickListener = clickListener;
            this.name = (TextView) view.findViewById(C0394R.id.name);
            view.setOnClickListener(new C04431(CategoriesArrayAdapter.this));
        }

        public void update(RCategory item, int position) {
            this.mPosition = position;
            this.name.setText(item.getName());
            this.name.setTextColor(ImageUtils.getCategoryTextColor(CategoriesArrayAdapter.this.getContext()));
            if (CategoriesArrayAdapter.this.mSelectedItem == position) {
                this.name.setBackgroundResource(ImageUtils.getCategoryBg());
            } else {
                this.name.setBackgroundColor(0);
            }
        }
    }

    public interface IOnClickListener {
        void onClick(int i);
    }

    public CategoriesArrayAdapter(Context context) {
        super(context);
        this.mSelectedItem = 0;
    }

    public void setOnCategoryClickListener(IOnClickListener clickListener) {
        this.mClickListener = clickListener;
    }

    public void setSelectedItem(int position) {
        this.mSelectedItem = position;
        notifyDataSetChanged();
    }

    public String getSelectedCategoryAction() {
        if (this.mSelectedItem == -1 || getItemCount() == 0) {
            return null;
        }
        return ((RCategory) getItem(this.mSelectedItem)).getAction();
    }

    public int getSelectedCategoryIndex() {
        return this.mSelectedItem;
    }

    public CategoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new CategoryViewHolder(getLayoutInflater().inflate(C0394R.layout.item_category, viewGroup, false), this.mClickListener);
    }

    public void onBindViewHolder(CategoryViewHolder vh, int pos) {
        vh.update((RCategory) getItem(pos), pos);
    }
}
