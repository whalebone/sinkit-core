package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tkozel on 3.8.15.
 */
public class VirusTotalRequest implements Serializable {

    private static final long serialVersionUID = -6504838382962619288L;

    private VirusTotalRequestStatus status;

    @SerializedName("processed")
    private Date processed;

    @SerializedName("report_received")
    private Date reportReceived;

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

    @Override
    public String toString() {
        return "VirusTotalRequest{" +
                "status=" + status +
                ", processed=" + processed +
                ", reportReceived=" + reportReceived +
                '}';
    }
}
