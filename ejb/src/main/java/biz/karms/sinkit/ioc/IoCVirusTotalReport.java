package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import com.kanishka.virustotal.dto.FileScanReport;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class IoCVirusTotalReport implements Serializable {

    private static final long serialVersionUID = -3335848811246446743L;

    private String fqdn;

    private String ip;

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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
                ", ip='" + ip + '\'' +
                ", scanDate=" + scanDate +
                ", urlReport=" + urlReport +
                '}';
    }
}
