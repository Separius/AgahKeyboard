package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
public class SoftException extends Exception {
    public SoftException(String message) {
        super(message);
    }

    public SoftException(String message, Throwable cause) {
        super(message, cause);
    }

    public SoftException(Throwable cause) {
        super(cause);
    }
}
