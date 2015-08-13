package biz.karms.sinkit.ejb.virustotal.exception;

/**
 * Created by tkozel on 4.8.15.
 */
public class VirusTotalException extends Exception {
    public VirusTotalException() {
        super();
    }

    public VirusTotalException(String message) {
        super(message);
    }

    public VirusTotalException(String message, Throwable cause) {
        super(message, cause);
    }

    public VirusTotalException(Throwable cause) {
        super(cause);
    }

    protected VirusTotalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
