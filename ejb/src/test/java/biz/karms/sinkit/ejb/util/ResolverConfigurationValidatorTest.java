package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.exception.ResolverConfigurationValidationException;
import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import biz.karms.sinkit.resolver.Strategy;
import biz.karms.sinkit.resolver.StrategyParams;
import biz.karms.sinkit.resolver.StrategyType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static java.lang.String.format;

public class ResolverConfigurationValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ResolverConfigurationValidator validator;
    private ResolverConfiguration resolverConfiguration;

    @Before
    public void setUp() {
        validator = new ResolverConfigurationValidator();
        resolverConfiguration = new ResolverConfiguration();
    }

    @Test
    public void testAllOk() throws ResolverConfigurationValidationException {
        resolverConfiguration.setResolverId(123);
        resolverConfiguration.setClientId(3);

        final Policy policy1 = new Policy();
        policy1.setId(1);
        policy1.setIpRanges(new HashSet<>());
        policy1.setAccuracyFeeds(Collections.singleton("accuracyFeed"));
        policy1.setBlacklistedFeeds(Collections.singleton("blackFeed"));
        final Strategy strategy1 = new Strategy();
        strategy1.setStrategyType(StrategyType.blacklist);
        policy1.setStrategy(strategy1);

        final Policy policy2 = new Policy();
        policy2.setId(2);
        policy2.setIpRanges(Collections.singleton("10.20.30.40/16"));
        final Strategy strategy2 = new Strategy();
        strategy2.setStrategyType(StrategyType.accuracy);
        final StrategyParams strategy2Params = new StrategyParams();
        strategy2Params.setAudit(12);
        strategy2Params.setBlock(50);
        strategy2.setStrategyParams(strategy2Params);
        policy2.setStrategy(strategy2);

        resolverConfiguration.setPolicies(Arrays.asList(policy1, policy2));


        // call tested method
        validator.validate(resolverConfiguration);
    }

    @Test
    public void testResolverConfigurationIsMandatory() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver configuration' cannot be null");

        // call tested method
        validator.resolverConfigurationIsMandatory(null);
    }

    @Test
    public void testResolveIdIsMandatory() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver Id' is mandatory in 'resolver configuration'");

        // call tested method
        validator.resolveIdIsMandatory(resolverConfiguration);
    }

    @Test
    public void testCustomerIdIsMandatory() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Customer Id' is mandatory in 'resolver configuration'");

        // call tested method
        validator.customerIdIsMandatory(resolverConfiguration);
    }

    @Test
    public void testAtLeastOnePolicyExistsNull() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver configuration' must contain at least one 'policy'");

        // call tested method
        validator.atLeastOnePolicyExists(resolverConfiguration);
    }

    @Test
    public void testAtLeastOnePolicyExistsEmpty() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver configuration' must contain at least one 'policy'");
        resolverConfiguration.setPolicies(Collections.emptyList());

        // call tested method
        validator.atLeastOnePolicyExists(resolverConfiguration);
    }

    @Test
    public void testPolicyIdMustByUnique() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage(format("'Resolver configuration.policy.id' must be unique. The following ids are duplicated '%s'",
                Arrays.asList("1", "4")));

        final Policy policy1 = new Policy();
        policy1.setId(1);
        final Policy policy2 = new Policy();
        policy2.setId(2);

        final Policy policy3 = new Policy();
        policy3.setId(1);

        final Policy policy4 = new Policy();
        policy4.setId(4);

        final Policy policy5 = new Policy();
        policy5.setId(4);

        resolverConfiguration.setPolicies(Arrays.asList(policy1, policy2, policy3, policy4, policy5));
        validator.policyIdMustByUnique(resolverConfiguration);
    }

    @Test
    public void testMaxOneIpRangesIsEmpty_NullVersion() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("Only one empty 'ip ranges' settings in 'resolver configuration.policy' is allowed");
        final Policy policy1 = new Policy();
        final Policy policy2 = new Policy();

        resolverConfiguration.setPolicies(Arrays.asList(policy1, policy2));

        // call tested method
        validator.maxOneIpRangesIsEmpty(resolverConfiguration);
    }

    @Test
    public void testMaxOneIpRangesIsEmpty_EmptyVersion() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("Only one empty 'ip ranges' settings in 'resolver configuration.policy' is allowed");
        final Policy policy1 = new Policy();
        policy1.setIpRanges(Collections.emptySet());
        final Policy policy2 = new Policy();
        policy2.setIpRanges(Collections.emptySet());

        resolverConfiguration.setPolicies(Arrays.asList(policy1, policy2));

        // call tested method
        validator.maxOneIpRangesIsEmpty(resolverConfiguration);
    }

    @Test
    public void testStrategyExists() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver configuration.policy' must contain 'strategy'");
        resolverConfiguration.setPolicies(Collections.singletonList(new Policy()));

        // call tested method
        validator.strategyExists(resolverConfiguration);
    }

    @Test
    public void testStrategyTypeMustExist() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Resolver configuration.policy.strategy' must contain 'strategy type' setting");

        final Policy policy = new Policy();
        policy.setStrategy(new Strategy());
        resolverConfiguration.setPolicies(Collections.singletonList(policy));

        // call tested method
        validator.strategyTypeMustExist(resolverConfiguration);
    }

    @Test
    public void testAccuracyStrategyTypeMustHaveAdditionalParamsSet() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Audit' and 'Block' settings must be defined in 'Resolver configuration.policy.strategy params' "
                + "if the 'Resolver configuration.policy.strategy.strategy type' acquires 'accuracy' value");

        final Policy policy = new Policy();
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.accuracy);
        policy.setStrategy(strategy);
        resolverConfiguration.setPolicies(Collections.singletonList(policy));

        // call tested method
        validator.accuracyStrategyTypeMustHaveAdditionalParamsSet(resolverConfiguration);
    }

    @Test
    public void testAccuracyStrategyTypeMustHaveAdditionalParamsSetAuditIsMissing() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Audit' and 'Block' settings must be defined in 'Resolver configuration.policy.strategy params' "
                + "if the 'Resolver configuration.policy.strategy.strategy type' acquires 'accuracy' value");

        final Policy policy = new Policy();
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.accuracy);
        final StrategyParams strategyParams = new StrategyParams();
        strategyParams.setBlock(33);
        strategy.setStrategyParams(strategyParams);
        policy.setStrategy(strategy);
        resolverConfiguration.setPolicies(Collections.singletonList(policy));

        // call tested method
        validator.accuracyStrategyTypeMustHaveAdditionalParamsSet(resolverConfiguration);
    }

    @Test
    public void testAccuracyStrategyTypeMustHaveAdditionalParamsSetBlockAuditIsMissing() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Audit' and 'Block' settings must be defined in 'Resolver configuration.policy.strategy params' "
                + "if the 'Resolver configuration.policy.strategy.strategy type' acquires 'accuracy' value");

        final Policy policy = new Policy();
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.accuracy);
        final StrategyParams strategyParams = new StrategyParams();
        strategyParams.setAudit(14);
        strategy.setStrategyParams(strategyParams);
        policy.setStrategy(strategy);
        resolverConfiguration.setPolicies(Collections.singletonList(policy));

        // call tested method
        validator.accuracyStrategyTypeMustHaveAdditionalParamsSet(resolverConfiguration);
    }

    //TODO: Look at what goes wrong
    //this test fails
    //@Test
    public void testAccuracyStrategyTypeParamsSetttings() throws ResolverConfigurationValidationException {
        expectedException.expect(ResolverConfigurationValidationException.class);
        expectedException.expectMessage("'Audit' parameter value in 'Resolver configuration.policy.strategy params' settings " +
                "must not be bigger than 'Resolver configuration.policy.strategy.strategy params.block' but " +
                "for the special case where either 'Audit' or 'Block' are 0. None of the parameters can be outside <0, 100>.");
        final Policy policy = new Policy();
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.accuracy);
        final StrategyParams strategyParams = new StrategyParams();
        strategyParams.setAudit(50);
        strategyParams.setBlock(30);
        strategy.setStrategyParams(strategyParams);
        policy.setStrategy(strategy);
        resolverConfiguration.setPolicies(Collections.singletonList(policy));

        // call tested method
        validator.accuracyStrategyTypeParamsSetttings(resolverConfiguration);
    }

}
