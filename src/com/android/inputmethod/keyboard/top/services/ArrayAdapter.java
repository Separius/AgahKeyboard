package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sepehr on 3/2/17.
 */

public abstract class ArrayAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private Context context;
    private LayoutInflater inflater;
    private final Object mLock;
    private boolean mNotifyOnChange;
    private List<T> objects;

    public ArrayAdapter(Context context) {
        this.mLock = new Object();
        this.mNotifyOnChange = true;
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(this.context);
        this.objects = new ArrayList();
    }

    public Context getContext() {
        return this.context;
    }

    public LayoutInflater getLayoutInflater() {
        return this.inflater;
    }

    public int getItemCount() {
        return this.objects.size();
    }

    public T getItem(int position) {
        return this.objects.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public int getPosition(T item) {
        return this.objects.indexOf(item);
    }

    public void add(T object) {
        synchronized (this.mLock) {
            this.objects.add(object);
        }
        if (this.mNotifyOnChange) {
            notifyItemInserted(this.objects.size() - 1);
        }
    }

    public void addAll(Collection<? extends T> collection) {
        int position = this.objects.size();
        synchronized (this.mLock) {
            this.objects.addAll(collection);
        }
        if (this.mNotifyOnChange) {
            notifyItemRangeInserted(position, collection.size());
        }
    }

    public void addAll(T... items) {
        int position = this.objects.size();
        synchronized (this.mLock) {
            Collections.addAll(this.objects, items);
        }
        if (this.mNotifyOnChange) {
            notifyItemRangeInserted(position, items.length);
        }
    }

    public void setItems(List<T> objects) {
        clear();
        addAll((Collection) objects);
    }

    public void insert(T object, int index) {
        synchronized (this.mLock) {
            this.objects.add(index, object);
        }
        if (this.mNotifyOnChange) {
            notifyItemInserted(index);
        }
    }

    public void remove(T object) {
        int position = this.objects.indexOf(object);
        synchronized (this.mLock) {
            this.objects.remove(object);
        }
        if (this.mNotifyOnChange && position != -1) {
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        int oldSize;
        synchronized (this.mLock) {
            oldSize = this.objects.size();
            this.objects.clear();
        }
        if (this.mNotifyOnChange) {
            notifyItemRangeRemoved(0, oldSize);
        }
    }

    public void sort(Comparator<? super T> comparator) {
        synchronized (this.mLock) {
            Collections.sort(this.objects, comparator);
        }
        if (this.mNotifyOnChange) {
            notifyItemRangeChanged(0, this.objects.size());
        }
    }

    public boolean isNotify() {
        return this.mNotifyOnChange;
    }

    public void setNotify(boolean mNotify) {
        this.mNotifyOnChange = mNotify;
    }
}

