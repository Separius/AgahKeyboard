package io.separ.neural.inputmethod.indic.setup;

import android.support.v4.app.Fragment;

/**
 * Created by sepehr on 1/27/17.
 */
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import io.separ.neural.inputmethod.indic.R;

public class WelcomeFragment extends AnimatedBackgroundGradientFragment {
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_welcome_layout, container, false);
        setupTransition((TransitionDrawable) view.getBackground());
        WebView webView = (WebView) view.findViewById(R.id.webber);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/particles.html");
        webView.setBackgroundColor(0);
        return view;
    }
}
