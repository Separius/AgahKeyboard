package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.separ.neural.inputmethod.indic.R;

public class WelcomeFragment extends AnimatedBackgroundGradientFragment {
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_welcome_layout, container, false);
        setupTransition((TransitionDrawable) view.getBackground());
        return view;
    }
}
