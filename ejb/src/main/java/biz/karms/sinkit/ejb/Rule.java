package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.util.BigIntegerTransformable;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import org.hibernate.search.annotations.*;
import org.hibernate.search.bridge.builtin.BigIntegerBridge;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
@Indexed
public class Rule implements Serializable {

    private static final long serialVersionUID = 2112325523047755691L;

    @NumericField
    @FieldBridge(impl = BigIntegerBridge.class)
    private BigIntegerTransformable startAddress;

    @NumericField
    @FieldBridge(impl = BigIntegerBridge.class)
    private BigIntegerTransformable endAddress;

    @Field
    private String cidrAddress;

    @IndexedEmbedded
    private List<String> sources;

    @Field
    @CalendarBridge(resolution = Resolution.HOUR)
    private Calendar created;

    @Field
    @CalendarBridge(resolution = Resolution.HOUR)
    private Calendar updated;

    @Field
    private String organization;

    @Field
    private String ruleName;

    /**
     * S for Sink / L for Log
     */
    @Field
    private char action;

    public Rule(String cidrAddress, List<String> sources, Calendar created, Calendar updated, String organization, String ruleName, char action) {
        this.sources = sources;
        this.created = created;
        this.updated = updated;
        this.organization = organization;
        this.ruleName = ruleName;
        this.action = action;
        setCidrAddress(cidrAddress);
    }

    public BigIntegerTransformable getStartAddress() {
        return startAddress;
    }

    public BigIntegerTransformable getEndAddress() {
        return endAddress;
    }

    public String getCidrAddress() {
        return cidrAddress;
    }

    public void setCidrAddress(String cidrAddress) {
        this.cidrAddress = cidrAddress;
        //TODO: Setter for CIDR Utils? Utils Factory?
        CIDRUtils cidrUtils = null;
        try {
            cidrUtils = new CIDRUtils(cidrAddress);
        } catch (UnknownHostException e) {
            this.startAddress = null;
            this.endAddress = null;
            e.printStackTrace();
        }
        this.startAddress = new BigIntegerTransformable(cidrUtils.getStartIp().toString());
        this.endAddress = new BigIntegerTransformable(cidrUtils.getEndIp().toString());
        cidrUtils = null;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public Calendar getUpdated() {
        return updated;
    }

    public void setUpdated(Calendar updated) {
        this.updated = updated;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public char getAction() {
        return action;
    }

    public void setAction(char action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "startAddress=" + startAddress +
                ", endAddress=" + endAddress +
                ", cidrAddress='" + cidrAddress + '\'' +
                ", sources=" + sources +
                ", created=" + created +
                ", updated=" + updated +
                ", organization='" + organization + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", action=" + action +
                '}';
    }
}
