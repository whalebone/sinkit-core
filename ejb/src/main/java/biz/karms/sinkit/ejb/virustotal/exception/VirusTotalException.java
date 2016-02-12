package biz.karms.sinkit.ejb.virustotal.exception;

/**
 * Created by tkozel on 4.8.15.
 */
public class VirusTotalException extends Exception {

    private boolean apiCalled = false;

    public VirusTotalException(String message, boolean apiCalled) {
        super(message);
        this.apiCalled = apiCalled;
    }

    public VirusTotalException(String message, Throwable cause, boolean apiCalled) {
        super(message, cause);
        this.apiCalled = apiCalled;
    }

    public VirusTotalException(Throwable cause, boolean apiCalled) {
        super(cause);
        this.apiCalled = apiCalled;
    }

    protected VirusTotalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public boolean isApiCalled() {
        return apiCalled;
    }
}
