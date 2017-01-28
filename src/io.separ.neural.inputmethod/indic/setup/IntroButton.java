package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.Button;

import io.separ.neural.inputmethod.indic.R;

public class IntroButton extends Button {
    private ButtonState state;

    private enum ButtonState {
        TODO,
        DONE
    }

    public IntroButton(Context context) {
        super(context);
        this.state = ButtonState.TODO;
        init();
    }

    public IntroButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.state = ButtonState.TODO;
        init();
    }

    public IntroButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.state = ButtonState.TODO;
        init();
    }

    @RequiresApi(api = 21)
    public IntroButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.state = ButtonState.TODO;
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.intro_button_background);
        setTextColor(-1);
    }

    public void setCompletedState() {
        this.state = ButtonState.DONE;
        setTextColor(Color.parseColor("#80FFFFFF"));
        setBackgroundResource(R.drawable.intro_button_completed);
    }

    public boolean isCompleted() {
        return this.state == ButtonState.DONE;
    }
}