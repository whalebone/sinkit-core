package biz.karms.sinkit.ejb;

import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
@Indexed
public class Rule implements Serializable {

    private static final long serialVersionUID = 2112324523047755691L;

    @Field
    private String startAddress;

    @Field
    private String endAddress;

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
     * S for Sink / L for Log / D for Disable
     */
    @Field
    private char action;

    public Rule(String startAddress, String endAddress, String cidrAddress, List<String> sources, Calendar created, Calendar updated, String organization, String ruleName, char action) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.cidrAddress = cidrAddress;
        this.sources = sources;
        this.created = created;
        this.updated = updated;
        this.organization = organization;
        this.ruleName = ruleName;
        this.action = action;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public String getCidrAddress() {
        return cidrAddress;
    }

    public void setCidrAddress(String cidrAddress) {
        this.cidrAddress = cidrAddress;
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
                "startAddress='" + startAddress + '\'' +
                ", endAddress='" + endAddress + '\'' +
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
