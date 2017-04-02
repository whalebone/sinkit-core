package biz.karms.sinkit.exception;

/**
 * @author Tomas Kozel
 */
public class TooOldIoCException extends IoCValidationException {

    public TooOldIoCException() {
        super();
    }

    public TooOldIoCException(String message) {
        super(message);
    }

    public TooOldIoCException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooOldIoCException(Throwable cause) {
        super(cause);
    }

    protected TooOldIoCException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
