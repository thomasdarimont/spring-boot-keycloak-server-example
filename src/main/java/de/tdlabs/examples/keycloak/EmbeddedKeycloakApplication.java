package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Created by tom on 12.06.16.
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

  static KeycloakServerProperties keycloakServerProperties;

  public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
    super(context, dispatcher);

    tryCreateMasterRealmAdminUser();
  }

  private void tryCreateMasterRealmAdminUser() {

    KeycloakSession session = getSessionFactory().create();

    ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

    String adminUsername = keycloakServerProperties.getAdminUsername();
    String adminPassword = keycloakServerProperties.getAdminPassword();

    try {
      session.getTransactionManager().begin();
      applianceBootstrap.createMasterRealmUser(adminUsername, adminPassword);
      session.getTransactionManager().commit();
    } catch (Exception ex) {
      System.out.println("Couldn't create keycloak master admin user: " + ex.getMessage());
      session.getTransactionManager().rollback();
    }

    session.close();
  }

}
