package biz.karms.sinkit.exception;

/**
 * Created by tkozel on 25.6.15.
 */
public class ArchiveException extends Exception {

    public ArchiveException() {
    }

    public ArchiveException(String message) {
        super(message);
    }

    public ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchiveException(Throwable cause) {
        super(cause);
    }

    public ArchiveException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
