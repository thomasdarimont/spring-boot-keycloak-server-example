package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by tom on 12.06.16.
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

  static KeycloakServerProperties keycloakServerProperties;

  public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
    super(augmentToRedirectContextPath(context), dispatcher);

    tryCreateMasterRealmAdminUser();
    tryImportExistingKeycloakFile();
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

  private void tryImportExistingKeycloakFile() {
    KeycloakSession session = getSessionFactory().create();

    URL url = getClass().getResource(keycloakServerProperties.getUsersConfigurationFile());
    if (url != null && new File(url.getPath()).exists()) {
      ExportImportConfig.setAction("import");
      ExportImportConfig.setProvider("singleFile");
      ExportImportConfig.setFile(url.getPath());

      ExportImportManager manager = new ExportImportManager(session);
      manager.runImport();
    }

    session.close();
  }

  static ServletContext augmentToRedirectContextPath(ServletContext servletContext) {

    ClassLoader classLoader = servletContext.getClassLoader();
    Class[] interfaces = {ServletContext.class};

    InvocationHandler invocationHandler = (proxy, method, args) -> {

      if ("getContextPath".equals(method.getName())) {
        return keycloakServerProperties.getContextPath();
      }

      if ("getInitParameter".equals(method.getName()) && args.length == 1 && "keycloak.embedded".equals(args[0])) {
        return "true";
      }

      return method.invoke(servletContext, args);
    };

    return ServletContext.class.cast(Proxy.newProxyInstance(classLoader, interfaces, invocationHandler));
  }
}
