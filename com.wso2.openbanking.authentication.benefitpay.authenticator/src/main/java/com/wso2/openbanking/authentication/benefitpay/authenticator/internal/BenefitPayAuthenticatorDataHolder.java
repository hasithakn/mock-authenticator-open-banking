package com.wso2.openbanking.authentication.benefitpay.authenticator.internal;

import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.multi.attribute.login.mgt.MultiAttributeLoginService;

import java.util.Properties;

public class BenefitPayAuthenticatorDataHolder {

    private static final BenefitPayAuthenticatorDataHolder instance = new BenefitPayAuthenticatorDataHolder();
    private MultiAttributeLoginService multiAttributeLogin;
    private Properties recaptchaConfigs;
    private ConfigurationManager configurationManager = null;
    public MultiAttributeLoginService getMultiAttributeLogin() {
        return multiAttributeLogin;
    }
    private BenefitPayAuthenticatorDataHolder() {
    }

    public static BenefitPayAuthenticatorDataHolder getInstance() {
        return instance;
    }
    public void setMultiAttributeLogin(MultiAttributeLoginService multiAttributeLogin) {
        this.multiAttributeLogin = multiAttributeLogin;
    }

    public Properties getRecaptchaConfigs() {
        return recaptchaConfigs;
    }

    public void setRecaptchaConfigs(Properties recaptchaConfigs) {
        this.recaptchaConfigs = recaptchaConfigs;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }
}
