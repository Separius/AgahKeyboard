package io.separ.neural.inputmethod.indic.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.inputmethod.keyboard.top.services.SimpleItemTouchHelperCallback;
import com.android.inputmethod.latin.utils.LeakGuardHandlerWrapper;
import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;

import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 3/10/17.
 */
public final class SetupWizardActivity extends Activity {
    private static final String STATE_STEP = "step";
    private static final int STEP_1 = 1;
    private static final int STEP_2 = 2;
    private static final int STEP_3 = 3;
    private static final int STEP_BACK_FROM_IME_SETTINGS = 5;
    private static final int STEP_LAUNCHING_IME_SETTINGS = 4;
    static final String TAG;
    private SettingsPoolingHandler mHandler;
    private InputMethodManager mImm;
    private boolean mNeedsToAdjustStepNumberToSystemState;
    private View mSetupWizard;
    private StepManager mStepManager;
    private int mStepNumber;

    private static final class SettingsPoolingHandler extends LeakGuardHandlerWrapper<SetupWizardActivity> {
        private static final long IME_SETTINGS_POLLING_INTERVAL = 200;
        private static final int MSG_POLLING_IME_SETTINGS = 0;
        private final InputMethodManager mImmInHandler;

        public SettingsPoolingHandler(SetupWizardActivity ownerInstance, InputMethodManager imm) {
            super(ownerInstance);
            this.mImmInHandler = imm;
        }

        public void handleMessage(Message msg) {
            SetupWizardActivity setupWizardActivity = getOwnerInstance();
            if (setupWizardActivity != null) {
                switch (msg.what) {
                    case 0:
                        if (UncachedInputMethodManagerUtils.isThisImeEnabled(setupWizardActivity, this.mImmInHandler)) {
                            setupWizardActivity.invokeSetupWizardOfThisIme();
                        } else {
                            startPollingImeSettings();
                        }
                    default:
                }
            }
        }

        public void startPollingImeSettings() {
            sendMessageDelayed(obtainMessage(0), IME_SETTINGS_POLLING_INTERVAL);
        }

        public void cancelPollingImeSettings() {
            removeMessages(0);
        }
    }

    private class StepManager {
        final int[] buttonsTexts;
        final Runnable[] clickActions;
        private PointF focusPoint;
        final int[] imageIds;
        final int[] infos;
        private Context mContext;

        /* renamed from: co.touchlab.inputmethod.latin.setup.SetupWizardActivity.StepManager.1 */
        class C04891 implements Runnable {
            C04891() {
            }

            public void run() {
                SetupWizardActivity.this.invokeLanguageAndInputSettings();
                SetupWizardActivity.this.mHandler.startPollingImeSettings();
            }
        }

        /* renamed from: co.touchlab.inputmethod.latin.setup.SetupWizardActivity.StepManager.2 */
        class C04902 implements Runnable {
            C04902() {
            }

            public void run() {
                SetupWizardActivity.this.invokeInputMethodPicker();
            }
        }

        /* renamed from: co.touchlab.inputmethod.latin.setup.SetupWizardActivity.StepManager.3 */
        class C04913 implements Runnable {
            C04913() {
            }

            public void run() {
                SetupWizardActivity.this.invokeSettingsOfThisIme();
                SetupWizardActivity.this.finish();
            }
        }

        /* renamed from: co.touchlab.inputmethod.latin.setup.SetupWizardActivity.StepManager.4 */
        class C04924 implements View.OnClickListener {
            final /* synthetic */ int val$position;

            C04924(int i) {
                this.val$position = i;
            }

            public void onClick(View v) {
                StepManager.this.clickActions[this.val$position].run();
            }
        }

        public StepManager(Context context) {
            this.imageIds = new int[]{R.drawable.ic_launcher_keyboard, R.drawable.ic_launcher_keyboard, R.drawable.ic_launcher_keyboard};
            Runnable[] runnableArr = new Runnable[SetupWizardActivity.STEP_3];
            runnableArr[0] = new C04891();
            runnableArr[SetupWizardActivity.STEP_1] = new C04902();
            runnableArr[SetupWizardActivity.STEP_2] = new C04913();
            this.clickActions = runnableArr;
            this.buttonsTexts = new int[]{R.string.enable_slash, R.string.switch_to_slash, R.string.use_slash};
            this.infos = new int[]{R.string.enable_slash_info, R.string.switch_to_slash_info, R.string.use_slash_info};
            this.mContext = context;
            this.focusPoint = new PointF(0.5f, SimpleItemTouchHelperCallback.ALPHA_FULL);
        }

        public void setCurrentItem(int position) {
            SimpleDraweeView bigImage = (SimpleDraweeView) SetupWizardActivity.this.mSetupWizard.findViewById(R.id.page_image);
            TextView pageNumber = (TextView) SetupWizardActivity.this.mSetupWizard.findViewById(R.id.page_number);
            Button slashButton = (Button) SetupWizardActivity.this.mSetupWizard.findViewById(R.id.page_button);
            TextView stepInfo = (TextView) SetupWizardActivity.this.mSetupWizard.findViewById(R.id.step_info);
            bigImage.setImageURI(resourceToUri(this.imageIds[position]));
            ((GenericDraweeHierarchy) bigImage.getHierarchy()).setActualImageFocusPoint(this.focusPoint);
            pageNumber.setText((position + SetupWizardActivity.STEP_1) + "");
            slashButton.setText(this.mContext.getText(this.buttonsTexts[position]));
            slashButton.setOnClickListener(new C04924(position));
            stepInfo.setText(new SpannableStringBuilder(this.mContext.getText(this.infos[position])));
        }

        private Uri resourceToUri(int resID) {
            return new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path(String.valueOf(resID)).build();
        }
    }

    public SetupWizardActivity() {
        this.mStepNumber = STEP_1;
    }

    static {
        TAG = SetupWizardActivity.class.getSimpleName();
    }

    public static void startActivity(Context context) {
        Intent activityIntent = new Intent(context, SetupWizardActivity.class);
        activityIntent.setFlags(268435456);
        context.startActivity(activityIntent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*AnalyticsHelper.create(this);
        AnalyticsHelper.track(getString(C0394R.string.mixpanel_onboarding_begins), new String[0]);*/
        this.mImm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        this.mHandler = new SettingsPoolingHandler(this, this.mImm);
        super.setContentView(R.layout.setup_wizard);
        this.mSetupWizard = findViewById(R.id.setup_wizard);
        if (savedInstanceState == null) {
            this.mStepNumber = determineSetupStepNumberFromLauncher();
        } else {
            this.mStepNumber = savedInstanceState.getInt(STATE_STEP);
        }
        this.mStepManager = new StepManager(this);
        updateSetupStepView();
    }

    void invokeSetupWizardOfThisIme() {
        Intent intent = new Intent();
        intent.setClass(this, SetupWizardActivity.class);
        intent.setFlags(606076928);
        startActivity(intent);
        this.mNeedsToAdjustStepNumberToSystemState = true;
    }

    private void invokeSettingsOfThisIme() {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        intent.setFlags(69206016);
        startActivity(intent);
    }

    void invokeLanguageAndInputSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
        intent.addCategory("android.intent.category.DEFAULT");
        startActivity(intent);
        this.mNeedsToAdjustStepNumberToSystemState = true;
    }

    void invokeInputMethodPicker() {
        this.mImm.showInputMethodPicker();
        this.mNeedsToAdjustStepNumberToSystemState = true;
    }

    void invokeSubtypeEnablerOfThisIme() {
        InputMethodInfo imi = UncachedInputMethodManagerUtils.getInputMethodInfoOf(getPackageName(), this.mImm);
        if (imi != null) {
            Intent intent = new Intent();
            intent.setAction("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra("android.intent.extra.TITLE", getString(R.string.language_selection_title));
            intent.putExtra("input_method_id", imi.getId());
            startActivity(intent);
        }
    }

    private int determineSetupStepNumberFromLauncher() {
        int stepNumber = determineSetupStepNumber();
        if (stepNumber == STEP_1) {
            return STEP_1;
        }
        if (stepNumber == STEP_3) {
            return STEP_LAUNCHING_IME_SETTINGS;
        }
        return stepNumber;
    }

    private int determineSetupStepNumber() {
        this.mHandler.cancelPollingImeSettings();
        if (!UncachedInputMethodManagerUtils.isThisImeEnabled(this, this.mImm)) {
            return STEP_1;
        }
        if (UncachedInputMethodManagerUtils.isThisImeCurrent(this, this.mImm)) {
            return STEP_3;
        }
        return STEP_2;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, this.mStepNumber);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mStepNumber = savedInstanceState.getInt(STATE_STEP);
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }

    private static boolean isInSetupSteps(int stepNumber) {
        return stepNumber >= STEP_1 && stepNumber <= STEP_3;
    }

    protected void onRestart() {
        super.onRestart();
        if (isInSetupSteps(this.mStepNumber)) {
            this.mStepNumber = determineSetupStepNumber();
        }
    }

    protected void onResume() {
        super.onResume();
        if (this.mStepNumber == STEP_LAUNCHING_IME_SETTINGS) {
            this.mSetupWizard.setVisibility(View.GONE);
            invokeSettingsOfThisIme();
            this.mStepNumber = STEP_BACK_FROM_IME_SETTINGS;
        } else if (this.mStepNumber == STEP_BACK_FROM_IME_SETTINGS) {
            finish();
        } else {
            updateSetupStepView();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && this.mNeedsToAdjustStepNumberToSystemState) {
            this.mNeedsToAdjustStepNumberToSystemState = false;
            this.mStepNumber = determineSetupStepNumber();
            updateSetupStepView();
        }
    }

    private void updateSetupStepView() {
        this.mSetupWizard.setVisibility(View.VISIBLE);
        this.mStepManager.setCurrentItem(Math.min(this.mStepNumber, STEP_3) - 1);
    }
}
