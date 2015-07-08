package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;

import java.io.Serializable;

/**
 * Created by tkozel on 24.6.15.
 */
@Indexed
public class IoCSource implements Serializable {

    private static final long serialVersionUID = 2184815523047755695L;

    @Field
    private String url;

    @Field
    private String ip;

    @Field
    @SerializedName("domain_name")
    private String domainName;

    @Field
    @SerializedName("reverse_domain_name")
    private String reverseDomainName;

    @Field
    @NumericField
    private Integer asn;

    @Field
    @SerializedName("asn_prefix")
    private String asnName;

    @Field
    private IoCGeolocation geolocation;

    @Field
    @SerializedName("bgp_prefix")
    private String bgpPrefix;

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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    @Override
    public String toString() {
        return "IoCSource{" +
                "url='" + url + '\'' +
                ", ip='" + ip + '\'' +
                ", domainName='" + domainName + '\'' +
                ", reverseDomainName='" + reverseDomainName + '\'' +
                ", asn=" + asn +
                ", asnName='" + asnName + '\'' +
                ", geolocation=" + geolocation +
                ", bgpPrefix='" + bgpPrefix + '\'' +
                '}';
    }
}
