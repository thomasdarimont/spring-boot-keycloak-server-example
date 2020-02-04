package eu.europeana.keycloak.apikey;

import eu.europeana.keycloak.StaticPropertyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.springframework.util.StringUtils;

import java.util.*;

public class EuropeanaEventListenerProviderFactory extends StaticPropertyUtil implements EventListenerProviderFactory {

    private static final Logger                   LOG              = LogManager.getLogger(
            EuropeanaEventListenerProviderFactory.class);
    private static final Set<EventType>           SUPPORTED_EVENTS = new HashSet<>();
    private static final String                   APKEYSERVICEURL  = "Apikey service URL";
    private static final String                   APIKEYMGID       = "Apikey manager client ID";
    private static final String                   APIKEYMGSECRET   = "Apikey manager client secret";
    private static final String                   NOTINSYSENV      = "not found in System environment. Checking keycloak-server.json... ";
    private static final String                   NOTINJSON        = "not found. Checking keycloak properties file ... ";
    private static final String                   NOTHEREEITHER    = "nothing there either. Clients synchronisation will not be possible.";

    static {
        // These are the events for which we will update the access date!
        Collections.addAll(SUPPORTED_EVENTS, EventType.CLIENT_LOGIN, EventType.REFRESH_TOKEN);
    }

    private Set<EventType> includedEvents = new HashSet<>();
    private String         apikeyServiceURL;
    private String         clientId;
    private String         clientSecret;


    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        LOG.info("Creating EuropeanaEventListenerProvider ...");
        return new EuropeanaEventListenerProvider(keycloakSession, includedEvents, apikeyServiceURL, clientId, clientSecret);
    }

    @Override
    public void init(Config.Scope scope) {
        LOG.info("EuropeanaEventListenerProviderFactory init .... ");
        StringBuilder message = new StringBuilder();
        String[] include = scope.getArray("include-events");
        if (include != null) {
            Arrays.stream(include).map(s -> EventType.valueOf(s.toUpperCase())).forEach(eventType -> includedEvents.add(eventType));
        } else {
            includedEvents.addAll(SUPPORTED_EVENTS);
        }

        String[] exclude = scope.getArray("exclude-events");
        if (exclude != null) {
            Arrays.stream(exclude).map(s -> EventType.valueOf(s.toUpperCase())).forEach(eventType -> includedEvents.remove(eventType));
        }

        // first try environment variable
        apikeyServiceURL = System.getenv("APIKEY_SERVICE_URL");
        message.append("Reading " + APKEYSERVICEURL + " ... ");
        if (StringUtils.isEmpty(apikeyServiceURL)) {
            // fallback read it from keycloak-server.json
            message.append(NOTINSYSENV);
            apikeyServiceURL = scope.get("apikey-service-url");
            if (StringUtils.isEmpty(apikeyServiceURL)) {
                // second fallback: properties file (added in order to run locally)
                message.append(NOTINJSON);
                apikeyServiceURL = StaticPropertyUtil.getApikeyServiceUrl();
                if (StringUtils.isEmpty(apikeyServiceURL)) {
                    message.append(NOTHEREEITHER);
                }
            }
        }

        if (apikeyServiceURL == null || apikeyServiceURL.isEmpty()) {
            LOG.warn(message.toString());
        } else {
            LOG.info("{} found: {}", APKEYSERVICEURL, apikeyServiceURL);
        }

        if (apikeyServiceURL != null && !apikeyServiceURL.startsWith("https")) {
            LOG.warn("Connection to API Key service is not over SSL. Synchronisation will be disabled.");
        }

        message.setLength(0);
        clientId = System.getenv("APIKEY_MANAGER_CLIENT_ID");
        message.append("Reading " + APIKEYMGID + " ... ");
        if (StringUtils.isEmpty(clientId)) {
            message.append(NOTINSYSENV);
            clientId = scope.get("client-id");
            if (StringUtils.isEmpty(clientId)) {
                message.append(NOTINJSON);
                clientId = StaticPropertyUtil.getApikeyManagerClientId();
                if (StringUtils.isEmpty(clientId)) {
                    message.append(NOTHEREEITHER);
                }
            }
        }
        if (clientId == null || clientId.isEmpty()) {
            LOG.warn(message.toString());
        } else {
            LOG.info("{} found (hidden)", APIKEYMGID);
        }

        message.setLength(0);
        clientSecret = System.getenv("APIKEY_MANAGER_CLIENT_SECRET");
        message.append("Reading " + APIKEYMGSECRET + " ... ");
        if (StringUtils.isEmpty(clientSecret)) {
            message.append(NOTINSYSENV);
            clientSecret = scope.get("client-secret");
            if (StringUtils.isEmpty(clientSecret)) {
                message.append(NOTINJSON);
                clientSecret = StaticPropertyUtil.getApikeyManagerClientSecret();
                if (StringUtils.isEmpty(clientSecret)) {
                    message.append(NOTHEREEITHER);
                }
            }
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            LOG.warn(message.toString());
        } else {
            LOG.info("{} found (hidden)", APIKEYMGSECRET);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "EuropeanaEventListener";
    }
}
