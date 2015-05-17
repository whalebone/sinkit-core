package biz.karms.sinkit.ejb;

import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Michal Karm Babacek
 */
@Indexed
public class BlacklistedRecord implements Serializable {

    private static final long serialVersionUID = 2184815523047755691L;

    @Field
    private String source;

    @Field
    @CalendarBridge(resolution = Resolution.HOUR)
    private Calendar listed;

    @Field(analyze = Analyze.YES)
    private String blackListedDomainOrIP;

    @Field
    @NumericField
    private int taxonomy;

    @Field
    @NumericField
    private int score;

    public BlacklistedRecord(String source, Calendar listed, String blackListedDomainOrIP, int taxonomy, int score) {
        this.source = source;
        this.listed = listed;
        this.blackListedDomainOrIP = blackListedDomainOrIP;
        this.taxonomy = taxonomy;
        this.score = score;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Calendar getListed() {
        return listed;
    }

    public void setListed(Calendar listed) {
        this.listed = listed;
    }

    public String getBlackListedDomainOrIP() {
        return blackListedDomainOrIP;
    }

    public void setBlackListedDomainOrIP(String blackListedDomainOrIP) {
        this.blackListedDomainOrIP = blackListedDomainOrIP;
    }

    public int getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(int taxonomy) {
        this.taxonomy = taxonomy;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "BlacklistedRecord{" +
                "source='" + source + '\'' +
                ", listed=" + listed +
                ", blackListedDomainOrIP='" + blackListedDomainOrIP + '\'' +
                ", taxonomy=" + taxonomy +
                ", score=" + score +
                '}';
    }
}
