package de.tdlabs.examples.keycloak;

import eu.europeana.keycloak.StaticPropertyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
@EnableConfigurationProperties(KeycloakServerProperties.class)
@PropertySource(value = "classpath:keycloak.properties")
@PropertySource(value = "classpath:keycloak-user.properties", ignoreResourceNotFound = true)
public class EmbeddedKeycloakApp {

    private static final Logger LOG   = LogManager.getLogger(EmbeddedKeycloakApp.class);

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedKeycloakApp.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(ServerProperties serverProperties) {
        return evt -> {
            Integer port = serverProperties.getPort();
            String rootContextPath = serverProperties.getContextPath();
            String keycloakContextPath = StaticPropertyUtil.getContextPath();

            LOG.info("Embedded Keycloak started: http://localhost:{}{}{} to use keycloak", port, rootContextPath, keycloakContextPath);
        };
    }
}
