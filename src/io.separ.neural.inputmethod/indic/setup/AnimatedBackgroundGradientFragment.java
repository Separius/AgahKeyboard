package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */

import android.graphics.drawable.TransitionDrawable;
import android.support.v4.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AnimatedBackgroundGradientFragment extends Fragment {
    private boolean hasBeenScheduled;
    private Timer timer;

    public class TransitionTimerTask extends TimerTask {
        int f19i;
        private final TransitionDrawable trans;

        /* renamed from: org.smc.inputmethod.indic.appintro.AnimatedBackgroundGradientFragment.TransitionTimerTask.1 */
        class C06001 implements Runnable {
            C06001() {
            }

            public void run() {
                if (TransitionTimerTask.this.f19i % 2 == 0) {
                    TransitionTimerTask.this.trans.startTransition(3000);
                } else {
                    TransitionTimerTask.this.trans.reverseTransition(3000);
                }
                TransitionTimerTask transitionTimerTask = TransitionTimerTask.this;
                transitionTimerTask.f19i++;
            }
        }

        TransitionTimerTask(TransitionDrawable trans) {
            this.f19i = 0;
            this.trans = trans;
        }

        public void run() {
            if (AnimatedBackgroundGradientFragment.this.getActivity() != null) {
                AnimatedBackgroundGradientFragment.this.getActivity().runOnUiThread(new C06001());
            }
        }
    }

    public AnimatedBackgroundGradientFragment() {
        this.timer = new Timer();
        this.hasBeenScheduled = false;
    }

    public void setupTransition(TransitionDrawable background) {
        TransitionTimerTask transitionTimerTask = new TransitionTimerTask(background);
        if (!this.hasBeenScheduled) {
            this.hasBeenScheduled = true;
            this.timer.schedule(transitionTimerTask, 0, 3000);
        }
    }
}

