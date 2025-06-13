package com.wso2.openbanking.authentication.benefitpay.authenticator;

import com.wso2.openbanking.accelerator.consent.mgt.service.impl.ConsentCoreServiceImpl;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.inbound.InboundConstants;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.oauth.cache.SessionDataCache;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
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
        Property clientId = new Property();
        clientId.setName("test_config");
        clientId.setDisplayName("Test config");
        clientId.setRequired(true);
        clientId.setDescription("Test config");
        clientId.setType("string");
        clientId.setDisplayOrder(1);
        configProperties.add(clientId);

        return configProperties;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {
        String sessionDataKey = context.getCallerSessionKey();

        // todo : do validations

//        String userIdentifier = request.getParameter("userIdentifier");
//        String mobileNumber = request.getParameter("mobileNumber");
//        log.info("userIdentifier: " + userIdentifier);
//        log.info("mobileNumber: " + mobileNumber);

        // validate client has specific scopes

//        List<String> requestObjectList = Arrays.stream(context.getQueryParams().split("&"))
//                .filter(e -> e.startsWith("request")).collect(Collectors.toList());
//        String requestObject = requestObjectList.get(0).split("=")[1];
//        log.info("sessionDataKey: " + sessionDataKey);
//        log.info("requestObject: " + requestObject);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MEDIA_TYPE_JSON);

        JSONObject resp = new JSONObject();
        resp.appendField("sessionId", sessionDataKey);
        resp.appendField("contextId", context.getContextIdentifier());
        resp.appendField("scope", request.getParameter("scope"));

        PrintWriter out = null;
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

        log.info("processResp");
        String callerSessionKey = authenticationContext.getCallerSessionKey();
        SessionDataCacheEntry valueFromCache = SessionDataCache.getInstance().getValueFromCache(new SessionDataCacheKey(callerSessionKey));
        log.info("callerSessionKey: " + callerSessionKey);
        AuthenticatedUser authenticatedUser =
                AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin@wso2.com");
        authenticationContext.setSubject(authenticatedUser);
        authenticatedUser.setFederatedIdPName("bhob-authenticator");

        valueFromCache.setLoggedInUser(authenticationContext.getLastAuthenticatedUser());

        Map<String, Serializable> endpointParams = valueFromCache.getEndpointParams();
        endpointParams.put("isError", false);
        endpointParams.put("loggedInUser", "admin@wso2.com@carbon.super");
        endpointParams.put("application", authenticationContext.getServiceProviderName());
        endpointParams.put("spQueryParams", authenticationContext.getQueryParams());
        endpointParams.put("scope", "openid accounts");
        endpointParams.put("tenantDomain", "carbon.super");

        invokeRetrieval(callerSessionKey);
        String persistPayload = "{\"approval\":\"true\",\"authorize\":false, \"accountIds\": [\"67890\"]}";
        try {
            invokePersist(callerSessionKey, persistPayload);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    public static void invokeRetrieval(String callerSessionKey) throws RuntimeException {
        String baseURL = "https://localhost:9446/api/openbanking/consent/authorize/retrieve";

        try {
            URL url = new URL(baseURL + "/" + URLEncoder.encode(callerSessionKey, "UTF-8"));
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "Basic YWRtaW5Ad3NvMi5jb206d3NvMjEyMw==");

            if (httpURLConnection.getResponseCode() == 200) {
                if (log.isDebugEnabled()) {
                    log.debug("Consent retrieved successfully.");
                }
            } else {
                log.error("Error while retrieving consent." + "Status code:" + httpURLConnection.getResponseCode()
                        + "Status message:" + httpURLConnection.getResponseMessage());
            }
        } catch (IOException e) {
            log.error(e);
        }
    }


    private static void invokePersist(String callerSessionKey, String consentData)
            throws RuntimeException, UnsupportedEncodingException {

        String baseURL = "https://localhost:9446/api/openbanking/consent/authorize/persist/" +
                URLEncoder.encode(callerSessionKey, "UTF-8") + "?authorize=false";
        log.info("Persist base url " + baseURL);
        JSONParser parser = new JSONParser();
        JSONObject persistPayload = null;
        try {
            persistPayload = (JSONObject) parser.parse(consentData);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        String approval = "true";
        persistPayload.put("approval", approval);
        String payload = persistPayload.toJSONString();
        log.debug("Persist Payload " + payload);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create the PATCH request
            log.debug("Inside try");
            HttpPatch httpPatch = new HttpPatch(baseURL);

            // Set the headers (Content-Type and Authorization, if necessary)
            httpPatch.setHeader("Content-Type", "application/json");
            httpPatch.setHeader("Authorization", "Basic YWRtaW5Ad3NvMi5jb206d3NvMjEyMw==");

            // Set the request body (JSON data)
            StringEntity entity = new StringEntity(payload);
            httpPatch.setEntity(entity);

            // Execute the PATCH request
            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                // print each header in response
                for (org.apache.http.Header header : response.getAllHeaders()) {
                    log.debug("Response Header: " + header.getName() + " = " + header.getValue());
                }
                log.debug("Inside try 2");
                // Check the response code
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                if (statusCode == 200) {

                    log.info("Consent persisted successfully. Status Code " + statusCode);
                } else {
                    log.info("Response Code: " + statusCode);
                    log.info("Response Body: " + responseString);
                    throw new RuntimeException("Error while persisting consent.");
                }
            }
        } catch (IOException e) {
            log.error(" Exception " + e);
            throw new RuntimeException("Error while persisting consent.");
        }

    }

    @Override
    public boolean canHandle(HttpServletRequest httpServletRequest) {
        // todo add token validation.
        return httpServletRequest.getParameter("proceedAuthorization") != null;
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
