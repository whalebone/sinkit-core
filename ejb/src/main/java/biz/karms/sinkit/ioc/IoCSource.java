package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
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

    public IoCSourceId getId() {
        return id;
    }

    public void setId(IoCSourceId id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFQDN() {
        return fqdn;
    }

    public void setFQDN(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getReverseDomainName() {
        return reverseDomainName;
    }

    public void setReverseDomainName(String reverseDomainName) {
        this.reverseDomainName = reverseDomainName;
    }

    public Integer getAsn() {
        return asn;
    }

    public void setAsn(Integer asn) {
        this.asn = asn;
    }

    public String getAsnName() {
        return asnName;
    }

    public void setAsnName(String asnName) {
        this.asnName = asnName;
    }

    public IoCGeolocation getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(IoCGeolocation geolocation) {
        this.geolocation = geolocation;
    }

    public String getBgpPrefix() {
        return bgpPrefix;
    }

    public void setBgpPrefix(String bgpPrefix) {
        this.bgpPrefix = bgpPrefix;
    }

    public Long getTTL() {
        return ttl;
    }

    public void setTTL(Long ttl) {
        this.ttl = ttl;
    }

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
