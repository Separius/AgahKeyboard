package io.separ.neural.inputmethod.indic.setup;

import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import io.separ.neural.inputmethod.indic.R;

public class SmartKeyboardFragment extends AnimatedBackgroundGradientFragment {
    private AppCompatCheckBox checkBox;

    /* renamed from: org.smc.inputmethod.indic.appintro.SmartKeyboardFragment.1 */
    static class C06081 implements OnCheckedChangeListener {
        final /* synthetic */ WebView val$webView;

        C06081(WebView webView) {
            this.val$webView = webView;
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            this.val$webView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_smart_layout, container, false);
        setupTransition((TransitionDrawable) view.getBackground());
        WebView webView = (WebView) view.findViewById(R.id.webber);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/particles.html");
        webView.setBackgroundColor(0);
        this.checkBox = (AppCompatCheckBox) view.findViewById(R.id.auth_checkbox);
        this.checkBox.setOnCheckedChangeListener(new C06081(webView));
        return view;
    }

    public void onDestroy() {
        super.onDestroy();
    }
}