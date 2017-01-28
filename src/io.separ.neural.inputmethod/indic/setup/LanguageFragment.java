package io.separ.neural.inputmethod.indic.setup;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils;

import io.separ.neural.inputmethod.indic.R;


public class LanguageFragment extends AnimatedBackgroundGradientFragment {
    private InputMethodManager mImm;
    private View root;

    /* renamed from: org.smc.inputmethod.indic.appintro.LanguageFragment.1 */
    class C06071 implements OnClickListener {
        C06071() {
        }

        public void onClick(View v) {
            LanguageFragment.this.invokeSubtypeEnablerOfThisIme();
        }
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        this.root = inflater.inflate(R.layout.intro_language_layout, container, false);
        setupTransition((TransitionDrawable) this.root.getBackground());
        ((IntroButton) this.root.findViewById(R.id.language_button)).setOnClickListener(new C06071());
        return this.root;
    }

    void invokeSubtypeEnablerOfThisIme() {
        InputMethodInfo imi = UncachedInputMethodManagerUtils.getInputMethodInfoOf(getContext().getPackageName(), this.mImm);
        if (imi != null) {
            Intent intent = new Intent();
            intent.setAction("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra("input_method_id", imi.getId());
            startActivity(intent);
        }
        ((IntroActivity) this.getActivity()).onDonePressed(this);
    }
}
