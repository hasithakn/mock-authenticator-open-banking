package com.wso2.openbanking.authentication.benefitpay.authenticator.internal;

import com.wso2.openbanking.authentication.benefitpay.authenticator.BenefitPayAuthenticator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.multi.attribute.login.mgt.MultiAttributeLoginService;
import org.wso2.carbon.user.core.service.RealmService;

@Component(
        name = "com.wso2.openbanking.authentication.benefitpay.authenticator.internal.BenefitPayAuthenticatorServiceComponent",
        immediate = true)
public class BenefitPayAuthenticatorServiceComponent {

    private static final Log log = LogFactory.getLog(BenefitPayAuthenticatorServiceComponent.class);

    private static RealmService realmService;

    public static RealmService getRealmService() {

        return realmService;
    }

    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        log.debug("Setting the Realm Service");
        BenefitPayAuthenticatorServiceComponent.realmService = realmService;
    }

    @Activate
    protected void activate(ComponentContext ctxt) {

        try {
            BenefitPayAuthenticator benefitPayAuthenticator = new BenefitPayAuthenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), benefitPayAuthenticator, null);

        } catch (Throwable e) {
            log.error("BenefitPayAuthenticator bundle activation Failed", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {

        if (log.isDebugEnabled()) {
            log.info("BenefitPayAuthenticator bundle is deactivated");
        }
    }

    protected void unsetRealmService(RealmService realmService) {

        log.debug("UnSetting the Realm Service");
        BenefitPayAuthenticatorServiceComponent.realmService = null;
    }

    @Reference(
            name = "MultiAttributeLoginService",
            service = MultiAttributeLoginService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetMultiAttributeLoginService")
    protected void setMultiAttributeLoginService(MultiAttributeLoginService multiAttributeLogin) {

        BenefitPayAuthenticatorDataHolder.getInstance().setMultiAttributeLogin(multiAttributeLogin);
    }

    protected void unsetMultiAttributeLoginService(MultiAttributeLoginService multiAttributeLogin) {

        BenefitPayAuthenticatorDataHolder.getInstance().setMultiAttributeLogin(null);
    }

    @Reference(
            name = "resource.configuration.manager",
            service = ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigurationManager"
    )
    protected void registerConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the configuration manager in basic authenticator bundle.");
        }
        BenefitPayAuthenticatorDataHolder.getInstance().setConfigurationManager(configurationManager);
    }

    protected void unregisterConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting the configuration manager in basic authenticator bundle.");
        }
        BenefitPayAuthenticatorDataHolder.getInstance().setConfigurationManager(null);
    }
}
