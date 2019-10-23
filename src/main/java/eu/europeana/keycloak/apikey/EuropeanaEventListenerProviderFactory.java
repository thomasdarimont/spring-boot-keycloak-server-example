package eu.europeana.keycloak.apikey;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EuropeanaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EuropeanaEventListenerProviderFactory.class);

    private static final Set<EventType> SUPPORTED_EVENTS = new HashSet<EventType>();

    static {
        // These are the events for which we will update the access date!
        Collections.addAll(SUPPORTED_EVENTS, EventType.CLIENT_LOGIN, EventType.REFRESH_TOKEN);
    }

    private Set<EventType> includedEvents = new HashSet<EventType>();

    private String apikeyServiceURL;

    private String clientId;

    private String clientSecret;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new EuropeanaEventListenerProvider(keycloakSession, includedEvents, apikeyServiceURL, clientId, clientSecret);
    }

    @Override
    public void init(Config.Scope scope) {
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
        apikeyServiceURL = System.getenv("apikey_service_url");
        if (StringUtils.isEmpty(apikeyServiceURL)) {
            // fallback read it from keycloak-server.json
            apikeyServiceURL = scope.get("apikey-service-url");
        }
        LOG.info("Configured apikey-service-url is {}", apikeyServiceURL);
        if (apikeyServiceURL == null || apikeyServiceURL.isEmpty()) {
            LOG.warn("No apikey service URL provided. Clients synchronisation will not be possible");
        }

        if (apikeyServiceURL != null && !apikeyServiceURL.startsWith("https")) {
            LOG.warn("Connection to API Key service is not over SSL. Synchronisation will be disabled.");
        }

        clientId = System.getenv("apikey_manager_client_id");
        if (StringUtils.isEmpty(clientId)) {
            clientId = scope.get("client-id");
        }
        if (clientId == null || clientId.isEmpty()) {
            LOG.warn("No client id for synchronisation provided. Clients synchronisation will not be possible");
        }

        clientSecret = System.getenv("apikey_manager_client_secret");
        if (StringUtils.isEmpty(clientSecret)) {
            clientSecret = scope.get("client-secret");
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            LOG.warn("No client secret for synchronisation client provided. Clients synchronisation will not be possible");
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
