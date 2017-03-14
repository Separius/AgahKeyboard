package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.separ.neural.inputmethod.indic.R;

import static io.separ.neural.inputmethod.Utils.SizeUtils.pxFromDp;

public class EnableButtonLayout extends LinearLayout {
    private final String buttonText;
    private ImageView imageView;
    private IntroButton introButton;
    private final int number;
    private TextView numberView;

    public EnableButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EnableButtonLayout);
        this.buttonText = ta.getString(1);
        this.number = ta.getInteger(0, 0);
        ta.recycle();
        init();
    }

    public EnableButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EnableButtonLayout);
        this.buttonText = ta.getString(1);
        this.number = ta.getInteger(0, 0);
        ta.recycle();
        init();
    }

    @TargetApi(21)
    public EnableButtonLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EnableButtonLayout);
        this.buttonText = ta.getString(1);
        this.number = ta.getInteger(0, 0);
        ta.recycle();
        init();
    }

    private void init() {
        setWeightSum(9.0f);
        int padding = pxFromDp(getContext(), 10.0f);
        setGravity(17);
        this.numberView = new TextView(getContext());
        this.numberView.setText(this.number + ".");
        this.numberView.setTextColor(-1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            this.numberView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        }
        this.numberView.setTextSize(2, 17.0f);
        this.numberView.setLayoutParams(new LayoutParams(0, -2, 2.0f));
        this.introButton = new IntroButton(getContext());
        this.introButton.setText(this.buttonText);
        this.introButton.setLayoutParams(new LayoutParams(0, -2, 5.0f));
        this.imageView = new ImageView(getContext());
        this.imageView.setImageResource(R.drawable.minimal_tick);
        this.imageView.setLayoutParams(new LayoutParams(0, pxFromDp(getContext(), 40.0f), 2.0f));
        this.imageView.setVisibility(INVISIBLE);
        this.imageView.setPadding(padding / 2, padding, padding, padding);
        addView(this.numberView);
        addView(this.introButton);
        addView(this.imageView);
    }

    public void setCompletedState() {
        this.introButton.setCompletedState();
        this.imageView.setVisibility(VISIBLE);
    }

    public boolean isCompleted() {
        return this.introButton.isCompleted();
    }

    public void setOnClickListener(OnClickListener l) {
        this.introButton.setOnClickListener(l);
    }
}