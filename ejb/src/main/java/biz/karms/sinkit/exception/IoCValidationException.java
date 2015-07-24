package biz.karms.sinkit.exception;

/**
 * Created by tkozel on 24.7.15.
 */
public class IoCValidationException extends Exception {
    public IoCValidationException() {
        super();
    }

    public IoCValidationException(String message) {
        super(message);
    }

    public IoCValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IoCValidationException(Throwable cause) {
        super(cause);
    }

    protected IoCValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
