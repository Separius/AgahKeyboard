package io.separ.neural.inputmethod.Utils;

/**
 * Created by sepehr on 1/25/17.
 */
enum PaddingMode {
    LEFT(1),
    RIGHT(2),
    NO(0);

    private final int id;

    private PaddingMode(int id) {
        this.id = id;
    }

    public int getID() {
        return this.id;
    }

    public static PaddingMode getMode(int id) {
        switch (id) {
            case 1:
                return LEFT;
            case 2:
                return RIGHT;
            default:
                return NO;
        }
    }
}
