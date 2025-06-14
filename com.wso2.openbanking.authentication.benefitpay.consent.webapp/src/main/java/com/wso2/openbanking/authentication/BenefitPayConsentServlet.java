package com.wso2.openbanking.authentication;

import com.wso2.openbanking.accelerator.common.config.OpenBankingConfigParser;
import com.wso2.openbanking.accelerator.consent.extensions.common.ConsentCache;
import net.minidev.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The servlet responsible for displaying the consent details in the auth UI flow.
 */
public class BenefitPayConsentServlet extends HttpServlet {
    public static final String USERNAME_IN_OB_CONFIGS = "Consent.ConsentAPICredentials.Username";
    public static final String PASSWORD_IN_OB_CONFIGS = "Consent.ConsentAPICredentials.Password";
    private static Logger log = LoggerFactory.getLogger(BenefitPayConsentServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        boolean isBenefitPayFlow = false;
        // get consent data
        String sessionDataKey = request.getParameter("sessionDataKeyConsent");
        SessionDataCacheEntry cacheEntry =
                ConsentCache.getCacheEntryFromCacheKey(ConsentCache.getCacheKey(sessionDataKey));

        // Get sessionContext from Authenticator.
        SessionContext sessionContext =
                FrameworkUtils.getSessionContextFromCache(cacheEntry.getSessionContextIdentifier());

        if (sessionContext == null) {
            // SessionContext is null, which means the session has expired or not found.
        } else {
            Map<String, AuthenticatedIdPData> authenticatedIdPs = sessionContext.getAuthenticatedIdPs();
            if (authenticatedIdPs != null && authenticatedIdPs.containsKey("benefitpay-authenticator")) {
                isBenefitPayFlow = true;
            }
        }
        if (!isBenefitPayFlow) {
            // get invoked consent page url
            String currentConsentPageURL = constructCurrentConsentURL(request);

            // modify and redirect to regular open banking consent flow
            String path = currentConsentPageURL.replace("benefitpay", "ob");
            response.sendRedirect(path);
            return;

        } else {
            // BenefitPay consent flow
            // Retrieve the session context properties from authenticator.
            Map<String, String> sessionContextProperties = (Map<String, String>)
                    sessionContext.getProperty(FrameworkConstants.AUTHENTICATION_CONTEXT_PROPERTIES);
            String mobileNumber = sessionContextProperties.get("mobileNumber");
            String userIdentifier = sessionContextProperties.get("userIdentifier");
            String action = sessionContextProperties.get("action");
            String accounts = sessionContextProperties.get("accounts");

            HttpResponse consentDataResponse = getConsentDataWithKey(sessionDataKey, getServletContext());
            InputStream contentStream = consentDataResponse.getEntity().getContent();

            // retrieve the response body , this contains the consent details, accounts available to select etc.
            // in this flow accounts available for select is not necessary, so we can ignore it,
            // or we can remove it by customising existing Retrieval steps
            // (ref : https://ob.docs.wso2.com/en/latest/develop/consent-management-authorize/#retrieve)
            String retrievalResponseBody = new BufferedReader(new InputStreamReader(contentStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Invoke persist call to store the consent approval and redirect to the next step.
            boolean approval = true; //  this is based on the `action` params value, true for approve, false for reject.
            JSONObject persistPayload = new JSONObject();
            persistPayload.put("approval", approval);
            persistPayload.put("authorize", false);
            persistPayload.put("accountIds", new String[]{accounts});
            invokePersist(sessionDataKey, persistPayload, response);
        }
    }

    @NotNull
    private static String constructCurrentConsentURL(HttpServletRequest request) {
        // construct regular ob consent page URL
        String scheme = request.getScheme();             // http or https
        String serverName = request.getServerName();         // e.g. localhost
        int serverPort = request.getServerPort();         // e.g. 8080
        String contextPath = request.getContextPath();        // e.g. /myapp
        String servletPath = request.getServletPath();        // e.g. /cookie_policy.do
        String pathInfo = request.getPathInfo();           // optional extra path
        String queryString = request.getQueryString();        // e.g. ?lang=en

        // Construct base URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        // Include port only if it's not the default for the scheme
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);
        if (pathInfo != null) {
            url.append(pathInfo);
        }

        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    private static void invokePersist(String callerSessionKey, JSONObject persistPayload,
                                      HttpServletResponse originalResponse) {

        // todo : get these URL form configs.
        String baseURL = "https://localhost:9446/api/openbanking/consent/authorize/persist/" +
                URLEncoder.encode(callerSessionKey, StandardCharsets.UTF_8) + "?authorize=true";
        String payload = persistPayload.toJSONString();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(baseURL);
            // Set the headers (Content-Type and Authorization, if necessary)
            httpPatch.setHeader("Content-Type", "application/json");
            httpPatch.setHeader("Authorization", "Basic " + getConsentApiCredentials());
            // Set the request body (JSON data)
            StringEntity entity = new StringEntity(payload);
            httpPatch.setEntity(entity);
            // Execute the PATCH request
            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                // Check the response code
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP && response.getLastHeader("Location") != null) {
                    originalResponse.sendRedirect(response.getLastHeader("Location").getValue());
                    return;
                }
            }
        } catch (IOException e) {
            log.error(" Exception " + e);
        }

    }

    HttpResponse getConsentDataWithKey(String sessionDataKeyConsent, ServletContext servletContext) throws IOException {

        // todo : get these URL form configs.
        String retrievalBaseURL = "https://localhost:9446/api/openbanking/consent/authorize/retrieve";
        String retrieveUrl = retrievalBaseURL + "/" + sessionDataKeyConsent;
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet dataRequest = new HttpGet(retrieveUrl);
        dataRequest.addHeader("Authorization", "Basic " + getConsentApiCredentials());
        return client.execute(dataRequest);

    }

    static String getConsentApiCredentials() {
        String username, password;
        username = (String) OpenBankingConfigParser.getInstance().getConfiguration()
                .get(USERNAME_IN_OB_CONFIGS);
        password = (String) OpenBankingConfigParser.getInstance().getConfiguration()
                .get(PASSWORD_IN_OB_CONFIGS);
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
