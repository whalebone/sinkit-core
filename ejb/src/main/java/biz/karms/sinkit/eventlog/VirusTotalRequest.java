package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class VirusTotalRequest implements Serializable {

    private static final long serialVersionUID = -6504838382962619288L;

    @SerializedName("status")
    private VirusTotalRequestStatus status;

    @SerializedName("processed")
    private Date processed;

    @SerializedName("report_received")
    private Date reportReceived;

    @SerializedName("cause_of_failure")
    private String causeOfFailure;

    @SerializedName("failed_attempts")
    private Integer failedAttempts;

    @SerializedName("failed")
    private Date failed;

    public VirusTotalRequestStatus getStatus() {
        return status;
    }

    public void setStatus(VirusTotalRequestStatus status) {
        this.status = status;
    }

    public Date getProcessed() {
        return processed;
    }

    public void setProcessed(Date processed) {
        this.processed = processed;
    }

    public Date getReportReceived() {
        return reportReceived;
    }

    public void setReportReceived(Date reportReceived) {
        this.reportReceived = reportReceived;
    }

    public String getCauseOfFailure() {
        return causeOfFailure;
    }

    public void setCauseOfFailure(String causeOfFailure) {
        this.causeOfFailure = causeOfFailure;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Date getFailed() {
        return failed;
    }

    public void setFailed(Date failed) {
        this.failed = failed;
    }

    @Override
    public String toString() {
        return "VirusTotalRequest{" +
                "status=" + status +
                ", processed=" + processed +
                ", reportReceived=" + reportReceived +
                ", causeOfFailure='" + causeOfFailure + '\'' +
                ", failedAttempts=" + failedAttempts +
                ", failed=" + failed +
                '}';
    }
}
