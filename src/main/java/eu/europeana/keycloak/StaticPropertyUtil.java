package eu.europeana.keycloak;

import de.tdlabs.examples.keycloak.KeycloakServerProperties;
import eu.europeana.keycloak.apikey.EuropeanaEventListenerProviderFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * Created by luthien on 23/01/2020.
 */
public class StaticPropertyUtil {

    public StaticPropertyUtil(){}

    private static       KeycloakServerProperties kcProperties;

    static {
        kcProperties = getKcProperties();
    }

    protected static String getPepper(){
        return kcProperties.getPepper();
    }

    protected static String getApikeyServiceUrl(){
        return kcProperties.getApikeyServiceUrl();
    }

    protected static String getApikeyManagerClientId(){
        return kcProperties.getApikeyManagerClientId();
    }

    protected static String getApikeyManagerClientSecret(){
        return kcProperties.getApikeyManagerClientSecret();
    }

    protected static String getContextPath(){
        return kcProperties.getContextPath();
    }

    protected static KeycloakServerProperties.AdminUser getAdminUser() {
        return kcProperties.getAdminUser();
    }

    private static KeycloakServerProperties getKcProperties() {
        try {
            PropertiesFactoryBean factoryBean;
            factoryBean = new PropertiesFactoryBean();
            factoryBean.setSingleton(false);

            String path = Objects.requireNonNull(
                    EuropeanaEventListenerProviderFactory.class.getClassLoader()
                                                               .getResource("keycloak-user.properties")).getPath();
            FileSystemResource fsr = new FileSystemResource(path);
            factoryBean.setLocation(fsr);
            Properties allProperties = factoryBean.getObject();

            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addLast(new PropertiesPropertySource("kcProperties", allProperties));

            KeycloakServerProperties kcProperties = new KeycloakServerProperties();

            PropertiesConfigurationFactory<KeycloakServerProperties> configurationFactory = new PropertiesConfigurationFactory<>(kcProperties);
            configurationFactory.setPropertySources(propertySources);
            configurationFactory.setTargetName("keycloak.server");
            configurationFactory.bindPropertiesToTarget();

            return kcProperties;

        } catch (IOException | BindException e) {
            throw new IllegalArgumentException(e);

        }
    }
}
