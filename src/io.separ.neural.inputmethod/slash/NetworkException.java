package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
public class NetworkException extends SoftException {
    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }
}
