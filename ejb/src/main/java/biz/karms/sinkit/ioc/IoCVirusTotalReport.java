package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import com.kanishka.virustotal.dto.FileScanReport;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tkozel on 31.7.15.
 */
public class IoCVirusTotalReport implements Serializable {

    private static final long serialVersionUID = -3335848811246446743L;

    private String fqdn;

    @SerializedName("scan_date")
    private Date scanDate;

    @SerializedName("url_report")
    private FileScanReport urlReport;

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public void setScanDate(Date scanDate) {
        this.scanDate = scanDate;
    }

    public FileScanReport getUrlReport() {
        return urlReport;
    }

    public void setUrlReport(FileScanReport urlReport) {
        this.urlReport = urlReport;
    }

    @Override
    public String toString() {
        return "IoCVirusTotalReport{" +
                "fqdn='" + fqdn + '\'' +
                ", scanDate=" + scanDate +
                ", urlReport=" + urlReport +
                '}';
    }
}
