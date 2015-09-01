package biz.karms.sinkit.exception;

/**
 * Created by tkozel on 30.8.15.
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
