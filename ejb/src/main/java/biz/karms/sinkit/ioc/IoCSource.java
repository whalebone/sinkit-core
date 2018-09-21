package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCSource implements Serializable {

    private static final long serialVersionUID = 6742947300724661734L;

    private IoCSourceId id;
    private String url;
    private String ip;
    private String fqdn;
    private Long ttl;

    @SerializedName("reverse_domain_name")
    private String reverseDomainName;

    private Integer asn;

    @SerializedName("asn_name")
    private String asnName;

    private IoCGeolocation geolocation;

    @SerializedName("bgp_prefix")
    private String bgpPrefix;

    @Override
    public String toString() {
        return "IoCSource{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", ip='" + ip + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", ttl=" + ttl +
                ", reverseDomainName='" + reverseDomainName + '\'' +
                ", asn=" + asn +
                ", asnName='" + asnName + '\'' +
                ", geolocation=" + geolocation +
                ", bgpPrefix='" + bgpPrefix + '\'' +
                '}';
    }
}
