package biz.karms.sinkit.exception;

/**
 * Created by Tomas Kozel
 */
public class IoCSourceIdException extends IoCValidationException {

    public IoCSourceIdException() {
        super();
    }

    public IoCSourceIdException(String message) {
        super(message);
    }

    public IoCSourceIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public IoCSourceIdException(Throwable cause) {
        super(cause);
    }

    protected IoCSourceIdException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
