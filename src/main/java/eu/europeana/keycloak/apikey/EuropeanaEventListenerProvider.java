package eu.europeana.keycloak.apikey;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

public class EuropeanaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG   = LogManager.getLogger(EuropeanaEventListenerProvider.class);

    private Set<EventType> includedEvents;

    private String clientId;

    private static final ApikeySynchronizer apikeySynchronizer = new ApikeySynchronizer();

    private KeycloakSession keycloakSession;

    public EuropeanaEventListenerProvider(KeycloakSession keycloakSession, Set<EventType> includedEvents, String apikeyServiceURL, String clientId, String clientSecret) {
        LOG.info("EuropeanaEventListenerProvider created");
        this.keycloakSession = keycloakSession;
        this.includedEvents = includedEvents;
        this.clientId = clientId;
        synchronized (apikeySynchronizer) {
            apikeySynchronizer.init(apikeyServiceURL, clientId, clientSecret);
        }
    }

    @Override
    public void onEvent(Event event) {
        LOG.info("OnEvent 1");
        if (includedEvents.contains(event.getType()) && !clientId.equals(event.getClientId()) && apikeySynchronizer.isSynchronizationEnabled()) {
            try {
                apikeySynchronizer.updateAccessDate(event.getClientId());
            } catch (IOException | ApikeyNotFoundException e) {
                LOG.error("API key corresponding to client id {} could not be updated.", event.getClientId(), e);
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        LOG.info("OnEvent 2");
        if (ResourceType.CLIENT.equals(adminEvent.getResourceType()) && apikeySynchronizer.isSynchronizationEnabled()) {
            String tempClientId = null;
            boolean enabled;
            String clientIdentifier = adminEvent.getResourcePath().substring(adminEvent.getResourcePath().lastIndexOf('/') + 1);

            try {
                if (OperationType.DELETE.equals(adminEvent.getOperationType())) {
                    apikeySynchronizer.deleteClient(clientIdentifier);
                } else if (OperationType.UPDATE.equals(adminEvent.getOperationType())) {
                    if (includeRepresentation) {
                        try (JsonReader reader = Json.createReader(new StringReader(adminEvent.getRepresentation()))) {
                            JsonObject object = reader.readObject();
                            tempClientId = object.getString("clientId");
                            enabled = object.getBoolean("enabled");
                        }
                    } else {
                        ClientModel clientModel = keycloakSession.clientLocalStorage().getClientById(clientIdentifier, keycloakSession.realmLocalStorage().getRealm("Europeana"));
                        if (clientModel != null) {
                            tempClientId = clientModel.getClientId();
                            enabled = clientModel.isEnabled();
                        } else {
                            LOG.warn("Client with id: {} not found", clientIdentifier);
                            return;
                        }
                    }
                    apikeySynchronizer.synchronizeClient(tempClientId, clientIdentifier, enabled);
                }
            } catch (IOException | ApikeyNotFoundException e) {
                LOG.error("Synchronization of a client failed. Client id: {}, Keycloak client identifier: {}.", tempClientId, clientIdentifier, e);
            }
        }
    }

    @Override
    public void close() {
    }
}
