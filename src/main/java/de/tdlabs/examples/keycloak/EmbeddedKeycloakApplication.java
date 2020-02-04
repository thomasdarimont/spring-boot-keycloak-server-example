package de.tdlabs.examples.keycloak;

import de.tdlabs.examples.keycloak.KeycloakServerProperties.AdminUser;
import eu.europeana.keycloak.StaticPropertyUtil;
import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Created by tom on 12.06.16.
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

    private static final Logger LOG   = LogManager.getLogger(EmbeddedKeycloakApplication.class);

    public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        super(augmentToRedirectContextPath(context), dispatcher);
        tryCreateMasterRealmAdminUser();
    }

    private void tryCreateMasterRealmAdminUser() {

        KeycloakSession session = getSessionFactory().create();

        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

        AdminUser admin = StaticPropertyUtil.getAdminUser();

        try {
            session.getTransactionManager().begin();
            if (applianceBootstrap.isNoMasterUser()) {
                applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
                LOG.info("Admin user created");
            } else {
                LOG.info("Admin user already present");
            }
            session.getTransactionManager().commit();
        } catch (RuntimeException ex) {
            LOG.error("Couldn't create keycloak master admin user", ex);
            session.getTransactionManager().rollback();
        }

        session.close();
    }


    private static ServletContext augmentToRedirectContextPath(ServletContext servletContext) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {

            if ("getContextPath".equals(method.getName())) {
                return StaticPropertyUtil.getContextPath();
            }

            if ("getInitParameter".equals(method.getName()) && args.length == 1 && "keycloak.embedded".equals(args[0])) {
                return "true";
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("{} {}", method.getName(), Arrays.toString(args));
            }
            return method.invoke(servletContext, args);
        };


        ClassLoader classLoader = servletContext.getClassLoader();
        Class[] interfaces = {ServletContext.class};
        return (ServletContext) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
    }
}
