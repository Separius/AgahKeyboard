package io.separ.neural.inputmethod.Utils;

/**
 * Created by sepehr on 1/25/17.
 */

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.android.inputmethod.keyboard.Key;
import com.android.inputmethod.keyboard.PointerTracker;

public class SwipeUtils {
    private static final String TAG;
    private static Context context;
    private static SelectionChanger selectionChanger;
    private static SwipeType selectionDirection;
    private static boolean didAct;
    public static boolean changedLanguage;

    /* renamed from: com.android.inputmethodcommon.SwipeUtils.1 */
    static /* synthetic */ class C02571 {
        static final /* synthetic */ int[] $SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType;

        static {
            $SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType = new int[SwipeType.values().length];
            try {
                $SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType[SwipeType.LEFT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType[SwipeType.RIGHT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType[SwipeType.TOP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static final class GestureListener extends SimpleOnGestureListener {
        private static final float DIFFERENCE_FACTOR_THRESHOLD = 1.2f;
        private static final int LONG_PRESS_DURATION = 200;
        private static final float MIN_MOVE_THRESHOLD_MULTI = 1.0f;
        private static final int SENSIBILITY_CHANGE_DISTANCE = 2;
        private static final int SWIPE_THRESHOLD = 25;
        private static final int SWIPE_VELOCITY_THRESHOLD = 10;
        private static final Handler handler;
        private static boolean isLongPress;
        private static float lastMoveX;
        private static float lastMoveY;
        private static float longPressX;
        private static float longPressY;
        private static Runnable mLongPressed;
        private static Key onDownKey;
        private static float otherLastMoveX;
        private static float otherLastMoveY;
        private static float otherLongPressX;
        private static float otherLongPressY;
        private static Key otherOnDownKey;

        /* renamed from: com.android.inputmethodcommon.SwipeUtils.GestureListener.1 */
        static class C02581 implements Runnable {
            C02581() {
            }

            public void run() {
                GestureListener.isLongPress = true;
            }
        }

        static {
            isLongPress = false;
            onDownKey = null;
            lastMoveX = -1.0f;
            lastMoveY = -1.0f;
            otherOnDownKey = null;
            otherLastMoveX = -1.0f;
            otherLastMoveY = -1.0f;
            handler = new Handler();
            mLongPressed = new C02581();
            didAct = false;
        }

        private static boolean handleDown(MotionEvent event) {
            isLongPress = false;
            didAct = false;
            onDownKey = SwipeUtils.getKey(event);
            if (onDownKey != null && SwipeUtils.isHotGestureKey(onDownKey)) {
                PointerTracker.setGestureHandlingEnabledByUser(false);
                handler.postDelayed(mLongPressed, 200);
                longPressX = event.getX();
                longPressY = event.getY();
                lastMoveX = event.getX();
                lastMoveY = event.getY();
            }
            return false;
        }

        private static boolean handleUp(MotionEvent event) {
            handler.removeCallbacks(mLongPressed);
            boolean retVal = false;
            if(isLongPress == false && SwipeUtils.isHotGestureKey(onDownKey))
                retVal = shortSwipe(event);
            onDownKey = null;
            otherOnDownKey = null;
            isLongPress = false;
            retVal = retVal || didAct;
            didAct = false;
            PointerTracker.setGestureHandlingEnabledByUser(true);
            return retVal;
        }

        private static boolean handleMove(MotionEvent event) {
            if (!isLongPress) {
                return false;
            }
            if (onDownKey != null && onDownKey.isDelete()) {
                //TODO: selectionChanger.deleteLastWord();
                return false;
            }
            handleLongMove(event);
            return true;
        }

        private static boolean handleLongMove(MotionEvent event) {
            float distance = lastMoveX - event.getX();
            float minMove = (float) SizeUtils.pxFromDp(SwipeUtils.context, 5.0f);
            if (Math.abs(lastMoveY - event.getY()) > ((float) SizeUtils.pxFromDp(SwipeUtils.context, 10.0f))) {
                lastMoveX = event.getX();
                lastMoveY = event.getY();
                return false;
            } else if (Math.abs(distance) > minMove) {
                boolean retVal = SwipeUtils.longSwipe(onDownKey, distance < 0.0f ? SwipeType.RIGHT : SwipeType.LEFT, event.getRawY() < ((float) SizeUtils.getScreenHeightInPx(SwipeUtils.context)) - ((float) (SwipeUtils.selectionChanger.getKeyboardHeight() / 3)));
                lastMoveX = event.getX();
                lastMoveY = event.getY();
                return retVal;
            }
            return false;
        }

        public static boolean onTouch(MotionEvent event) {
            switch (event.getAction() & 255) {
                case 0:
                    return handleDown(event);
                case 1:
                    return handleUp(event);
                case 2:
                    return handleMove(event);
                default:
                    return false;
            }
        }

        public static boolean shortSwipe(MotionEvent event){
            try {
                float diffY = event.getY() - lastMoveY;
                float diffX = event.getX() - lastMoveX;
                float distance = (float) Math.sqrt(Math.pow((double) diffX, 2.0d) + Math.pow((double) diffY, 2.0d));
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (distance < DIFFERENCE_FACTOR_THRESHOLD * Math.abs(diffX) && Math.abs(diffX) > ((float) SizeUtils.pxFromDp(SwipeUtils.context, 25.0f)) && Math.abs(diffY) < ((float) SizeUtils.pxFromDp(SwipeUtils.context, 25.0f))) {
                        return SwipeUtils.swipe(onDownKey, diffX > 0.0f ? SwipeType.RIGHT : SwipeType.LEFT);
                    }
                } else if (distance < DIFFERENCE_FACTOR_THRESHOLD * Math.abs(diffY) && Math.abs(diffY) > ((float) SizeUtils.pxFromDp(SwipeUtils.context, 25.0f)) && Math.abs(diffX) < ((float) SizeUtils.pxFromDp(SwipeUtils.context, 25.0f))) {
                    return SwipeUtils.swipe(onDownKey, diffY > 0.0f ? SwipeType.BOTTOM : SwipeType.TOP);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }
    }

    public interface SelectionChanger {
        void changeSelection(int i, int i2);

        int getKeyboardHeight();

        boolean isSelectionEmpty();

        void moveCursorBack();

        void moveCursorNext();

        void restartInput();

        void changeLanguageNext();

        void changeLanguagePrev();

        void deleteLastWord();

        void deleteAllWords();
    }

    enum SwipeType {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    static {
        TAG = SwipeUtils.class.getSimpleName();
    }

    public static void init(SelectionChanger s, Context c) {
        selectionChanger = s;
        context = c.getApplicationContext();
    }

    public static boolean spaceBarSelection() {
        return true;
    }

    private static boolean isHotGestureKey(Key key) {
        return key != null && (key.isSpaceBar() || key.isDelete());
    }

    private static Key getKey(MotionEvent event) {
        return getKey(event, event.getActionIndex());
    }

    private static Key getKey(MotionEvent event, int actionIndex) {
        return PointerTracker.getPointerTracker(event.getPointerId(actionIndex)).getKeyOn((int) event.getX(), (int) event.getY());
    }

    private static boolean spaceBarSwipe(SwipeType type) {
        switch (C02571.$SwitchMap$com$android$inputmethodcommon$SwipeUtils$SwipeType[type.ordinal()]) {
            case 1:
                selectionChanger.changeLanguageNext();
                changedLanguage = true;
                break;
            case 2:
                selectionChanger.changeLanguagePrev();
                changedLanguage = true;
                break;
        }
        return false;
    }

    private static boolean deleteSwipe(SwipeType type) {
        if (type == SwipeType.LEFT)
            selectionChanger.deleteLastWord();
        if (type == SwipeType.TOP)
            selectionChanger.deleteAllWords();
        return false;
    }

    private static boolean swipe(Key key, SwipeType type) {
        if (key != null) {
            if (key.isDelete())
                return deleteSwipe(type);
            if (key.isSpaceBar())
                return spaceBarSwipe(type);
        }
        return false;
    }

    private static boolean longSwipe(Key key, SwipeType type, boolean shouldSelect) {
        if (key == null || !spaceBarSelection() || !key.isSpaceBar()) {
            return false;
        }
        if (shouldSelect) {
            if (selectionChanger.isSelectionEmpty()) {
                selectionDirection = type;
            }
            if (selectionDirection == SwipeType.RIGHT) {
                if (type == SwipeType.RIGHT) {
                    selectionChanger.changeSelection(0, 1);
                } else {
                    selectionChanger.changeSelection(0, -1);
                }
            } else if (type == SwipeType.RIGHT) {
                selectionChanger.changeSelection(1, 0);
            } else {
                selectionChanger.changeSelection(-1, 0);
            }
        } else if (type == SwipeType.RIGHT) {
            selectionChanger.moveCursorNext();
        } else {
            selectionChanger.moveCursorBack();
        }
        didAct = true;
        return true;
    }
}
