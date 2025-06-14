package com.wso2.openbanking.authentication.benefitpay.authenticator;

import com.wso2.openbanking.accelerator.consent.mgt.service.impl.ConsentCoreServiceImpl;
import net.minidev.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.inbound.InboundConstants;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.model.Property;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BenefitPayAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

    private static final Log log = LogFactory.getLog(BenefitPayAuthenticator.class);
    private static final ConsentCoreServiceImpl consentCoreService = new ConsentCoreServiceImpl();
    public static final String MEDIA_TYPE_JSON = "application/json";

    @Override
    public List<Property> getConfigurationProperties() {

        // Get the required configuration properties.
        List<Property> configProperties = new ArrayList<>();
        Property jwksEndpoint = new Property();
        jwksEndpoint.setName("JWKS_Endpoint");
        jwksEndpoint.setDisplayName("JWKS_Endpoint");
        jwksEndpoint.setRequired(true);
        jwksEndpoint.setDescription("JWKS_Endpoint to validate the signature of incoming requests from GW");
        jwksEndpoint.setType("string");
        jwksEndpoint.setDisplayOrder(1);
        configProperties.add(jwksEndpoint);
        return configProperties;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        String userIdentifier = request.getParameter("userIdentifier");
        String mobileNumber = request.getParameter("mobileNumber");


        // todo : custom validation logics ..

        JSONObject resp = new JSONObject();
        resp.appendField("sessionId", context.getContextIdentifier());
        resp.appendField("scope", request.getParameter("scope"));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MEDIA_TYPE_JSON);

        PrintWriter out;
        try {
            out = response.getWriter();
            out.print(resp.toJSONString());
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse,
                                                 AuthenticationContext authenticationContext)
            throws AuthenticationFailedException {


        String sessionId = httpServletRequest.getParameter("sessionDataKey");
        // JWT token from KONG GW
        String token = httpServletRequest.getParameter("token");

        // get the configured authenticator JWKS endpoint.
        Map<String, String> authenticatorProperties = authenticationContext.getAuthenticatorProperties();
        String jwksEndpoint = authenticatorProperties.get("JWKS_Endpoint");

        // todo : verify the signature of the token with configured JWKS endpoint or certificate.
        // todo : if signature verification fails, invalid authentication -> breaks the flow.

        // decode token and extract below params.
        String sessionIdFromToken = "decodedToken.sessionId";        //decodedToken -> get ("userIdentifier");
        // todo : validate sessionIdFromToken with sessionId from request,
        //  this is to make sure this token can only be used for this session.

        String userIdentifier = "decodedToken.user";        //decodedToken -> get ("userIdentifier");
        String mobileNumber = "decodedToken.mobileNumber";  //decodedToken -> get ("mobileNumber");
        String action = "decodedToken.action";              //decodedToken -> get ("action")
        String accounts = "decodedToken.accounts";          //decodedToken -> get ("accounts")

        // set these properties in the authentication context.

        Map<String, String> datasetToConstPage = new HashMap<>();
        datasetToConstPage.put("mobileNumber",mobileNumber);
        datasetToConstPage.put("userIdentifier", userIdentifier);
        datasetToConstPage.put("action", action);
        datasetToConstPage.put("accounts", accounts);
        authenticationContext.setProperty(FrameworkConstants.AUTHENTICATION_CONTEXT_PROPERTIES, datasetToConstPage);

        // set the user identifier in the context.
        AuthenticatedUser authenticatedUser = AuthenticatedUser
                .createFederateAuthenticatedUserFromSubjectIdentifier(userIdentifier);
        authenticationContext.setSubject(authenticatedUser);
        authenticatedUser.setFederatedIdPName(this.getName());

    }

    @Override
    public boolean canHandle(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter("token") != null;
    }

    @Override
    public String getContextIdentifier(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter(InboundConstants.RequestProcessor.CONTEXT_KEY);

    }

    @Override
    public String getName() {
        return "benefitpay-authenticator";
    }

    @Override
    public String getFriendlyName() {
        return "benefitpay-authenticator";
    }


}
