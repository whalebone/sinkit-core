package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.exception.EndUserConfigurationValidationException;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EndUserConfigurationValidator {

    public void validate(EndUserConfiguration endUserConfiguration) throws EndUserConfigurationValidationException {
        userIsMandatory(endUserConfiguration);
        clientIdIsMandatory(endUserConfiguration);
        policyIdIsMandatory(endUserConfiguration);
    }

    void userIsMandatory(EndUserConfiguration endUserConfiguration) throws EndUserConfigurationValidationException {
        if (StringUtils.isBlank(endUserConfiguration.getUserId())) {
            throw new EndUserConfigurationValidationException("'User id' cannot be null");
        }
    }

    void clientIdIsMandatory(EndUserConfiguration endUserConfiguration) throws EndUserConfigurationValidationException {
        if (endUserConfiguration.getClientId() == null) {
            throw new EndUserConfigurationValidationException("'Customer id' cannot be null");
        }
    }

    void policyIdIsMandatory(EndUserConfiguration endUserConfiguration) throws EndUserConfigurationValidationException {
        if (endUserConfiguration.getPolicyId() == null) {
            throw new EndUserConfigurationValidationException("'Policy id' cannot be null");
        }
    }


}
