package eu.europeana.keycloak.apikey;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EuropeanaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EuropeanaEventListenerProviderFactory.class);

    private static final Set<EventType> SUPPORTED_EVENTS = new HashSet<EventType>();

    static {
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

        apikeyServiceURL = scope.get("apikey-service-url");
        if (apikeyServiceURL == null || apikeyServiceURL.isEmpty()) {
            LOG.warn("No apikey service URL provided. Clients synchronisation will not be possible");
        }

        if (!apikeyServiceURL.startsWith("https")) {
            LOG.warn("Connection to API Key service is not over SSL. Synchronisation will be disabled.");
        }

        clientId = scope.get("client-id");
        if (clientId == null || clientId.isEmpty()) {
            LOG.warn("No client id for synchronisation provided. Clients synchronisation will not be possible");
        }

        clientSecret = scope.get("client-secret");
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
