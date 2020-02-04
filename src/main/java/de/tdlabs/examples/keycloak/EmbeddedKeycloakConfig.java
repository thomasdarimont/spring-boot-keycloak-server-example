package de.tdlabs.examples.keycloak;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.listeners.KeycloakSessionDestroyListener;
import org.keycloak.services.resources.KeycloakApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
class EmbeddedKeycloakConfig {

    private static final Logger LOG = LogManager.getLogger(EmbeddedKeycloakConfig.class);

    private static final String DB_URL      = "keycloak.connectionsJpa.dbUrl";
    private static final String DB_USERNAME = "keycloak.connectionsJpa.user";
    @SuppressWarnings("squid:S2068")
    private static final String DB_PASSWORD = "keycloak.connectionsJpa.password";

    @Bean
    ServletRegistrationBean keycloakJaxRsApplication(KeycloakServerProperties keycloakServerProperties,
                                                     DataSource dataSource) throws NamingException {
        mockJndiEnvironment(dataSource);

        // Cloud Foundry will set the DATABASE_URL variable when there is a database service, but not in the desired
        // format for the postgres driver
        String databaseUrl = System.getenv("DATABASE_URL");
        if (!StringUtils.isEmpty(databaseUrl)) {
            LOG.info("Reading database credentials from system environment...");
            setPostgresCredentials(databaseUrl);
        }

        ServletRegistrationBean servlet = new ServletRegistrationBean(new HttpServlet30Dispatcher());
        servlet.addInitParameter(KeycloakApplication.KEYCLOAK_EMBEDDED, "true");
        servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
                                 keycloakServerProperties.getContextPath());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
        servlet.addUrlMappings(keycloakServerProperties.getContextPath() + "/*");
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);

        return servlet;
    }

    /**
     * Cloud Foundry supplies a DATABASE_URL in the form of "postgres://username:password@hostname:port/databasename"
     * whereas the Postgres drivers expect them to be like "jdbc:postgresql://hostname:port/databasename?user=username&password=password"
     * or with the username and password provided as separate properties. Here we do the latter because Keycloak logging
     * will print the full jdbcUrl when connecting and we prefer the password not to be logged.
     *
     * @param databaseUrl
     */
    private void setPostgresCredentials(String databaseUrl) {
        Pattern       p  = Pattern.compile("(postgres://)(.*?):(.*?)@(.*)");
        Matcher       m  = p.matcher(databaseUrl);
        StringBuilder sb = new StringBuilder("jdbc:postgresql://");
        if (m.find() && m.groupCount() == 4) {
            sb.append(m.group(4));
            System.setProperty(DB_URL, sb.toString());
            System.setProperty(DB_USERNAME, m.group(2));
            System.setProperty(DB_PASSWORD, m.group(3));
        } else {
            throw new IllegalArgumentException("Unexpected database url format!");
        }
    }

    @Bean
    ServletListenerRegistrationBean<KeycloakSessionDestroyListener> keycloakSessionDestroyListener() {
        return new ServletListenerRegistrationBean<>(new KeycloakSessionDestroyListener());
    }

    @Bean
    FilterRegistrationBean keycloakSessionManagement(KeycloakServerProperties keycloakServerProperties) {

        FilterRegistrationBean filter = new FilterRegistrationBean();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new KeycloakSessionServletFilter());
        filter.addUrlPatterns(keycloakServerProperties.getContextPath() + "/*");

        return filter;
    }

    private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
        NamingManager.setInitialContextFactoryBuilder(env -> environment -> new InitialContext() {

            @Override
            public Object lookup(Name name) {
                return lookup(name.toString());
            }

            @Override
            public Object lookup(String name) {
                if ("spring/datasource".equals(name)) {
                    return dataSource;
                }
                return null;
            }

            @Override
            public NameParser getNameParser(String name) {
                return CompositeName::new;
            }

            @Override
            public void close() {
                //NOOP
            }
        });
    }
}
