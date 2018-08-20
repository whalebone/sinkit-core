package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import com.kanishka.virustotal.dto.FileScanReport;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCVirusTotalReport implements Serializable {

    private static final long serialVersionUID = -3335848811246446743L;

    private String fqdn;

    private String ip;

    @SerializedName("scan_date")
    private Date scanDate;

    @SerializedName("url_report")
    private FileScanReport urlReport;

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
