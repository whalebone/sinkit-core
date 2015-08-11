package biz.karms.sinkit.ejb.cache.pojo;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
@Indexed
public class CustomList implements Serializable {

    private static final long serialVersionUID = 11111111111111111L;

    /**
     * Client DNS server identification
     */
    @Field
    private String clientStartAddress;

    @Field
    private String clientEndAddress;

    @Field
    private String clientCidrAddress;

    /**
     * Customer identification
     */
    @Field
    private int customerId;


    /**
     * Either fqdn or listCidrAddress is set, not both.
     *
     * While, Black, Log, defined as either CIDR or FQDN
     */
    @Field
    private String fqdn;

    @Field
    private String listStartAddress;

    @Field
    private String listEndAddress;

    @Field
    private String listCidrAddress;


    /**
     * ```<B|W|L>``` stands for Black White Log
     */
    @Field
    private String whiteBlackLog;

    public String getClientStartAddress() {
        return clientStartAddress;
    }

    public void setClientStartAddress(String clientStartAddress) {
        this.clientStartAddress = clientStartAddress;
    }

    public String getClientEndAddress() {
        return clientEndAddress;
    }

    public void setClientEndAddress(String clientEndAddress) {
        this.clientEndAddress = clientEndAddress;
    }

    public String getClientCidrAddress() {
        return clientCidrAddress;
    }

    public void setClientCidrAddress(String clientCidrAddress) {
        this.clientCidrAddress = clientCidrAddress;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getListStartAddress() {
        return listStartAddress;
    }

    public void setListStartAddress(String listStartAddress) {
        this.listStartAddress = listStartAddress;
    }

    public String getListEndAddress() {
        return listEndAddress;
    }

    public void setListEndAddress(String listEndAddress) {
        this.listEndAddress = listEndAddress;
    }

    public String getListCidrAddress() {
        return listCidrAddress;
    }

    public void setListCidrAddress(String listCidrAddress) {
        this.listCidrAddress = listCidrAddress;
    }

    public String getWhiteBlackLog() {
        return whiteBlackLog;
    }

    public void setWhiteBlackLog(String whiteBlackLog) {
        this.whiteBlackLog = whiteBlackLog;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomList)) return false;

        CustomList that = (CustomList) o;

        if (customerId != that.customerId) return false;
        if (clientStartAddress != null ? !clientStartAddress.equals(that.clientStartAddress) : that.clientStartAddress != null)
            return false;
        if (clientEndAddress != null ? !clientEndAddress.equals(that.clientEndAddress) : that.clientEndAddress != null)
            return false;
        if (clientCidrAddress != null ? !clientCidrAddress.equals(that.clientCidrAddress) : that.clientCidrAddress != null)
            return false;
        if (fqdn != null ? !fqdn.equals(that.fqdn) : that.fqdn != null) return false;
        if (listStartAddress != null ? !listStartAddress.equals(that.listStartAddress) : that.listStartAddress != null)
            return false;
        if (listEndAddress != null ? !listEndAddress.equals(that.listEndAddress) : that.listEndAddress != null)
            return false;
        if (listCidrAddress != null ? !listCidrAddress.equals(that.listCidrAddress) : that.listCidrAddress != null)
            return false;
        return whiteBlackLog.equals(that.whiteBlackLog);

    }

    @Override
    public int hashCode() {
        int result = clientStartAddress != null ? clientStartAddress.hashCode() : 0;
        result = 31 * result + (clientEndAddress != null ? clientEndAddress.hashCode() : 0);
        result = 31 * result + (clientCidrAddress != null ? clientCidrAddress.hashCode() : 0);
        result = 31 * result + customerId;
        result = 31 * result + (fqdn != null ? fqdn.hashCode() : 0);
        result = 31 * result + (listStartAddress != null ? listStartAddress.hashCode() : 0);
        result = 31 * result + (listEndAddress != null ? listEndAddress.hashCode() : 0);
        result = 31 * result + (listCidrAddress != null ? listCidrAddress.hashCode() : 0);
        result = 31 * result + whiteBlackLog.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CustomList{" +
                "clientStartAddress='" + clientStartAddress + '\'' +
                ", clientEndAddress='" + clientEndAddress + '\'' +
                ", clientCidrAddress='" + clientCidrAddress + '\'' +
                ", customerId=" + customerId +
                ", fqdn='" + fqdn + '\'' +
                ", listStartAddress='" + listStartAddress + '\'' +
                ", listEndAddress='" + listEndAddress + '\'' +
                ", listCidrAddress='" + listCidrAddress + '\'' +
                ", whiteBlackLog='" + whiteBlackLog + '\'' +
                '}';
    }
}
