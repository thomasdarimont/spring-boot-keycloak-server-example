package eu.europeana.keycloak.apikey;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Base64;

/**
 * Class used for synchronizing clients state with Apikey service. URL to Apikey service should be specified in the keycloak.json
 * configuration file. So should be the credentials for the manager client authorized to perform synchronize calls.
 *
 */
class ApikeySynchronizer {

    private static final Logger LOG   = LogManager.getLogger(ApikeySynchronizer.class);

    // URL to Apikey service
    private String apikeyServiceURL = null;

    // Authorization header with manager client credentials used for each synchronize request
    private Header authorizationHeader = null;

    // Http client to perform calls to Apikey service
    private CloseableHttpClient httpClient;

    // When no SSL connection to API Key service it will be false
    private boolean synchronizationEnabled = true;

    ApikeySynchronizer() {
        httpClient = HttpClients.createDefault();
    }

    /**
     * Initializes this object with data retrieved from configuration file. Client id and its secret
     * are not stored but the authorization header is prepared based on them.
     *
     * @param apikeyServiceURL URL to Apikey service
     * @param clientId client id of the manager client
     * @param clientSecret secret corresponding to the client id
     */
    void init(String apikeyServiceURL, String clientId, String clientSecret) {
        LOG.info("Initialising Apikey synchroniser ...");
        if (this.apikeyServiceURL == null) {
            this.apikeyServiceURL = apikeyServiceURL;
        }
        if (this.authorizationHeader == null) {
            this.authorizationHeader = prepareAuthorizationHeader(clientId, clientSecret);
        }
        if (apikeyServiceURL == null) {
            LOG.warn("No API Key service configured. Synchronisation will be disabled.");
            synchronizationEnabled = false;
        } else if (!apikeyServiceURL.startsWith("https")) {
            LOG.warn("Connection to API Key service is not over SSL. Synchronisation will be disabled.");
            synchronizationEnabled = false;
        } else {
            LOG.info("Synchronizer initialized");
        }
    }

    /**
     * In order to avoid storing credentials the authorization header is prepared and encoded. It is used for
     * each call requiring basic client credentials authorization.
     *
     * @param clientId manager client id
     * @param clientSecret secret corresponding to the clien
     * @return Basic authorization header
     */
    @SuppressWarnings("squid:S2647") // we have not other option beside basic authentication at the moment,
    // additionally we make sure we communicate over https
    private Header prepareAuthorizationHeader(String clientId, String clientSecret) {
        LOG.info("Apikey synchroniser prepare authorization header");
        return new BasicHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
    }

    /**
     * Synchronize state of the client between Keycloak and the Apikey service. In case of deletion of the client
     * in Keycloak it is identified by its Keycloak identifier because the client id is not available anymore. When
     * the client is disabled or enabled the proper request is performed. Before the actual update call information
     * from Apikey service is retrieved to determine whether the update is actually needed.
     *
     * @param synchronizedClientId client id that is synchronized between Keycloak and Apikey service
     * @param synchronizedKeycloakId client identifier of this client in Keycloak
     * @param enabled flag indicating current state of the client in Keycloak
     */
    void synchronizeClient(String synchronizedClientId, String synchronizedKeycloakId, boolean enabled) throws IOException, ApikeyNotFoundException {
        LOG.info("Apikey synchroniser syncronize client");
        if (!synchronizationEnabled) {
            LOG.warn("Synchronization is disabled due to not secured connection API Key service");
            return;
        }
        LOG.debug("Starting synchronization...");

        boolean previousEnabled = isEnabled(synchronizedClientId);

        if (previousEnabled != enabled) {
            HttpRequestBase requestBase;
            if (enabled) {
                requestBase = prepareReenableRequest(synchronizedClientId, synchronizedKeycloakId);
                LOG.debug("Sending re-enable request...");
            } else {
                requestBase = prepareInvalidateRequest(synchronizedClientId);
                LOG.debug("Sending invalidate request...");
            }

            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(requestBase);
                if (response != null && response.getStatusLine().getStatusCode() > HttpStatus.SC_NO_CONTENT) {
                    LOG.warn("Synchronization for api key {} failed with status: {} and message: {}.",
                            synchronizedClientId,
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase());
                }
            } finally {
                if (response != null) {
                    try {
                        response.close();
                    } catch (IOException e) {
                        LOG.error("Close response failed.", e);
                    }
                }
            }
        }
    }

    /**
     * Prepares DELETE request that invalidates the api key corresponding the client id
     * @param synchronizedClientId client id to synchronize
     * @return delete request object
     */
    private HttpDelete prepareInvalidateRequest(String synchronizedClientId) {
        LOG.info("Apikey synchroniser preparing invalidate request");
        HttpDelete httpDelete = new HttpDelete(apikeyServiceURL + "/" + synchronizedClientId);
        httpDelete.addHeader(authorizationHeader);
        return httpDelete;
    }

    /**
     * Prepares POST request that re-enables the api key corresponding to the client id
     * @param synchronizedClientId client id to synchronize
     * @param synchronizedKeycloakId keycloak client identifier
     * @return post request object
     */
    private HttpPost prepareReenableRequest(String synchronizedClientId, String synchronizedKeycloakId) {
        LOG.info("Apikey synchroniser preparing re-enable request");
        HttpPost httpPost = new HttpPost(apikeyServiceURL + "/" + synchronizedClientId + "?keycloakId=" + synchronizedKeycloakId);
        httpPost.addHeader(authorizationHeader);
        httpPost.addHeader("Content-Type", "application/json");
        return httpPost;
    }

    /**
     * Check in Api key service whether the given client id is enabled. This is done using validate request.
     *
     * @param synchronizedClientId client id to check
     * @return true when client is enabled, false otherwise
     * @throws IOException in case of IO problems with sending request
     * @throws ApikeyNotFoundException when client id is not recognized as api key
     */
    private boolean isEnabled(String synchronizedClientId) throws IOException, ApikeyNotFoundException {
        LOG.info("Apikey synchroniser checking client in Apikey service");
        HttpPost httpPost = prepareValidateRequest(synchronizedClientId);
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_GONE) {
                    return false;
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    return true;
                }
            }
        }
        throw new ApikeyNotFoundException(synchronizedClientId);
    }

    /**
     * Prepares POST request that validates the client id as api key
     *
     * @param synchronizedClientId client id to validate
     * @return post request object
     */
    private HttpPost prepareValidateRequest(String synchronizedClientId) {
        LOG.info("Apikey synchroniser validate request");
        HttpPost httpPost = new HttpPost(apikeyServiceURL + "/validate");
        httpPost.addHeader("Authorization", "APIKEY " + synchronizedClientId);
        return httpPost;
    }

    /**
     * Completely remove api key corresponding to the client from the Apikey service.
     *
     * @param clientIdentifier identifier of the client in Keycloak
     * @throws IOException when problem with request execution occurs
     */
    void deleteClient(String clientIdentifier) throws IOException {
        LOG.info("Apikey synchroniser preparing delete client");
        HttpDelete httpDelete = new HttpDelete(apikeyServiceURL + "/synchronize/" + clientIdentifier);
        httpDelete.addHeader(authorizationHeader);
        CloseableHttpResponse response = null;
        try {
            LOG.debug("Sending delete request...");
            response = httpClient.execute(httpDelete);
            if (response != null && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                LOG.warn("Delete api key {} failed.", clientIdentifier);
            }
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOG.error("Close response failed", e);
                }
            }
        }
    }

    /**
     * Updates access date for an api key corresponding to the given client id.
     * This update is preformed using validate method in Apikey service.
     *
     * @param clientId client id to update
     */
    void updateAccessDate(String clientId) throws IOException, ApikeyNotFoundException {
        if (!synchronizationEnabled) {
            LOG.warn("Synchronization is disabled due to not secured connection API Key service");
            return;
        }
        LOG.debug("Updating access date...");
        isEnabled(clientId);
    }

    /**
     * Close the http client
     */
    @PreDestroy
    void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOG.error("Closing HTTP Client in {} failed.", this.getClass().getName(), e);
        }
    }

    /**
     * Check if synchronization is enabled
     * @return synchronizationEnabled flag value
     */
    public boolean isSynchronizationEnabled() {
        return synchronizationEnabled;
    }
}
