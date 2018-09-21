package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.exception.ResolverConfigurationValidationException;
import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import biz.karms.sinkit.resolver.Strategy;
import biz.karms.sinkit.resolver.StrategyType;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ApplicationScoped
public class ResolverConfigurationValidator {

    public void validate(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        resolverConfigurationIsMandatory(resolverConfiguration);
        resolveIdIsMandatory(resolverConfiguration);
        customerIdIsMandatory(resolverConfiguration);
        atLeastOnePolicyExists(resolverConfiguration);
        policyIdMustByUnique(resolverConfiguration);
        maxOneIpRangesIsEmpty(resolverConfiguration);
        strategyExists(resolverConfiguration);
        strategyTypeMustExist(resolverConfiguration);
        accuracyStrategyTypeMustHaveAdditionalParamsSet(resolverConfiguration);
        accuracyStrategyTypeParamsSetttings(resolverConfiguration);
    }

    void resolverConfigurationIsMandatory(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration == null) {
            throw new ResolverConfigurationValidationException("'Resolver configuration' cannot be null");
        }
    }

    void resolveIdIsMandatory(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration.getResolverId() == null) {
            throw new ResolverConfigurationValidationException("'Resolver Id' is mandatory in 'resolver configuration'");
        }
    }

    void customerIdIsMandatory(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration.getClientId() == null) {
            throw new ResolverConfigurationValidationException("'Customer Id' is mandatory in 'resolver configuration'");
        }
    }

    void atLeastOnePolicyExists(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration.getPolicies() == null || resolverConfiguration.getPolicies().isEmpty()) {
            throw new ResolverConfigurationValidationException("'Resolver configuration' must contain at least one 'policy'");
        }
    }

    void policyIdMustByUnique(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        final List<Integer> policiesIds = resolverConfiguration.getPolicies().stream().map(Policy::getId).collect(Collectors.toList());
        final Set<Integer> duplicities = policiesIds.stream().filter(i -> Collections.frequency(policiesIds, i) > 1).collect(Collectors.toSet());

        if (!duplicities.isEmpty()) {
            throw new ResolverConfigurationValidationException(
                    format("'Resolver configuration.policy.id' must be unique. The following ids are duplicated '%s'", duplicities));
        }
        System.out.println();
    }

    void maxOneIpRangesIsEmpty(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        final long count = resolverConfiguration.getPolicies().stream().filter(policy -> policy.getIpRanges() == null || policy.getIpRanges().size() == 0)
                .count();
        if (count > 1) {
            throw new ResolverConfigurationValidationException("Only one empty 'ip ranges' settings in 'resolver configuration.policy' is allowed");
        }
    }

    void strategyExists(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration.getPolicies().stream().anyMatch(policy -> policy.getStrategy() == null)) {
            throw new ResolverConfigurationValidationException("'Resolver configuration.policy' must contain 'strategy'");
        }
    }

    void strategyTypeMustExist(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        if (resolverConfiguration.getPolicies().stream().map(Policy::getStrategy).anyMatch(strategy -> strategy.getStrategyType() == null)) {
            throw new ResolverConfigurationValidationException("'Resolver configuration.policy.strategy' must contain 'strategy type' setting");
        }
    }

    void accuracyStrategyTypeMustHaveAdditionalParamsSet(ResolverConfiguration resolverConfiguration)
            throws ResolverConfigurationValidationException {
        final boolean hasError = resolverConfiguration.getPolicies().stream()
                .map(Policy::getStrategy)
                .filter(strategy -> StrategyType.accuracy == strategy.getStrategyType())
                .map(Strategy::getStrategyParams)
                .anyMatch(params -> params == null || params.getAudit() == null || params.getBlock() == null);

        if (hasError) {
            throw new ResolverConfigurationValidationException(
                    "'Audit' and 'Block' settings must be defined in 'Resolver configuration.policy.strategy params' "
                            + "if the 'Resolver configuration.policy.strategy.strategy type' acquires 'accuracy' value");
        }
    }

    void accuracyStrategyTypeParamsSetttings(ResolverConfiguration resolverConfiguration) throws ResolverConfigurationValidationException {
        final boolean hasError = resolverConfiguration.getPolicies().stream()
                .map(Policy::getStrategy)
                .filter(strategy -> StrategyType.accuracy == strategy.getStrategyType())
                .map(Strategy::getStrategyParams)
                .anyMatch(params -> (((params.getAudit() > params.getBlock()) && params.getAudit() != 0 && params.getBlock() != 0) ||
                        params.getAudit() > 100 ||
                        params.getAudit() < 0 ||
                        params.getBlock() > 100 ||
                        params.getBlock() < 0));

        if (hasError) {
            throw new ResolverConfigurationValidationException(
                    "'Audit' parameter value in 'Resolver configuration.policy.strategy params' settings " +
                            "must not be bigger than 'Resolver configuration.policy.strategy.strategy params.block' but " +
                            "for the special case where either 'Audit' or 'Block' are 0. None of the parameters can be outside <0, 100>.");
        }
    }
}
