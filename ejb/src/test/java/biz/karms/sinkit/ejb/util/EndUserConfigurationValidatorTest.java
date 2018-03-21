package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.exception.EndUserConfigurationValidationException;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EndUserConfigurationValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EndUserConfigurationValidator validator;
    private EndUserConfiguration endUserConfiguration;

    @Before
    public void setUp() {
        validator = new EndUserConfigurationValidator();
        endUserConfiguration = new EndUserConfiguration();
    }

    @Test
    public void testAllOk() throws EndUserConfigurationValidationException {
        endUserConfiguration.setPolicyId(123);
        endUserConfiguration.setClientId(234);
        endUserConfiguration.setUserId("user123");

        validator.validate(endUserConfiguration);
    }

    @Test
    public void testUserIsMandatory() throws EndUserConfigurationValidationException {
        // preparation
        endUserConfiguration.setUserId(null);
        expectedException.expect(EndUserConfigurationValidationException.class);
        expectedException.expectMessage("'User id' cannot be null");

        // call tested method
        validator.userIsMandatory(endUserConfiguration);
    }

    @Test
    public void testClientIdIsMandatory() throws EndUserConfigurationValidationException {
        // preparation
        endUserConfiguration.setClientId(null);
        expectedException.expect(EndUserConfigurationValidationException.class);
        expectedException.expectMessage("'Customer id' cannot be null");

        // call tested method
        validator.clientIdIsMandatory(endUserConfiguration);
    }

    @Test
    public void testPolicyIdIsMandatory() throws EndUserConfigurationValidationException {
        // preparation
        endUserConfiguration.setPolicyId(null);
        expectedException.expect(EndUserConfigurationValidationException.class);
        expectedException.expectMessage("'Policy id' cannot be null");

        // call tested method
        validator.policyIdIsMandatory(endUserConfiguration);
    }


}