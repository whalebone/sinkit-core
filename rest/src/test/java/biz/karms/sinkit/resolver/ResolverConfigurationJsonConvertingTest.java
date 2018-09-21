package biz.karms.sinkit.resolver;

import biz.karms.sinkit.ioc.IoCClassificationType;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;


public class ResolverConfigurationJsonConvertingTest {

    @Test
    public void testConvertJsonToObject() {
        final InputStream jsonFileInputStream = ResolverConfigurationJsonConvertingTest.class.getClassLoader()
                .getResourceAsStream("resolver_configuration.json");
        final ResolverConfiguration configuration = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                .fromJson(new InputStreamReader(jsonFileInputStream), ResolverConfiguration.class);


        assertThat(configuration, notNullValue());
        assertThat(configuration.getResolverId(), is(12));
        assertThat(configuration.getClientId(), is(2));
        assertThat(configuration.getPolicies(), notNullValue());
        assertThat(configuration.getPolicies(), hasSize(3));

        final List<Policy> policies = configuration.getPolicies();
        final Policy firstPolicy = policies.get(0);
        assertThat(firstPolicy.getId(), is(0));
        assertThat(firstPolicy.getIpRanges(), notNullValue());
        assertThat(firstPolicy.getIpRanges(), empty());
        assertThat(firstPolicy.getStrategy(), notNullValue());
        assertThat(firstPolicy.getStrategy().getStrategyType(), is(StrategyType.accuracy));
        assertThat(firstPolicy.getStrategy().getStrategyParams(), notNullValue());
        assertThat(firstPolicy.getStrategy().getStrategyParams().getAudit(), is(50));
        assertThat(firstPolicy.getStrategy().getStrategyParams().getBlock(), is(80));
        assertThat(firstPolicy.getStrategy().getStrategyParams().getTypes(), notNullValue());
        assertThat(firstPolicy.getStrategy().getStrategyParams().getTypes(), hasItems(IoCClassificationType.malware, IoCClassificationType.blacklist,
                IoCClassificationType.cc, IoCClassificationType.phishing));

        assertThat(firstPolicy.getAccuracyFeeds(), notNullValue());
        assertThat(firstPolicy.getAccuracyFeeds(), hasSize(4));
        assertThat(firstPolicy.getAccuracyFeeds(), hasItems("phishtank", "hybrid-analysis", "non-existent-feed", "urlquery"));
        assertThat(firstPolicy.getBlacklistedFeeds(), notNullValue());
        assertThat(firstPolicy.getBlacklistedFeeds(), hasSize(3));
        assertThat(firstPolicy.getBlacklistedFeeds(), hasItems("mfcr", "mfsk", "another-invalid-feed"));

        assertThat(firstPolicy.getCustomlists(), notNullValue());
        assertThat(firstPolicy.getCustomlists().getBlackList(), notNullValue());
        assertThat(firstPolicy.getCustomlists().getBlackList(), hasSize(1));
        assertThat(firstPolicy.getCustomlists().getBlackList(), hasItems("malware.com"));
        assertThat(firstPolicy.getCustomlists().getDropList(), notNullValue());
        assertThat(firstPolicy.getCustomlists().getDropList(), empty());
        assertThat(firstPolicy.getCustomlists().getWhiteList(), hasItems("whalebone.io", "google.com", "playmuseek.com"));
        assertThat(firstPolicy.getCustomlists().getAuditList(), empty());


        final Policy secondPolicy = policies.get(1);
        assertThat(secondPolicy.getId(), is(1));
        assertThat(secondPolicy.getIpRanges(), hasItems("10.10.0.0/16", "10.20.30.0/8", "2001:0db8:85a3:1234:5678:8a2e:0370:0/8"));
        assertThat(secondPolicy.getCustomlists().getAuditList(), hasItems("amoreshop.com.ua", "claymorebg.com"));

        final Policy thirdPolicy = policies.get(2);
        assertThat(thirdPolicy.getId(), is(2));
        assertThat(thirdPolicy.getCustomlists().getDropList(), hasItems("malware.com"));
    }
}
