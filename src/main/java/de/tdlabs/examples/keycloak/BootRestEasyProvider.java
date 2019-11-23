/**
 * 
 */
package de.tdlabs.examples.keycloak;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.servlet.ServletContext;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.util.ResteasyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mike
 */
public class BootRestEasyProvider implements ResteasyProvider {

	private static final Logger LOG = LoggerFactory.getLogger(BootRestEasyProvider.class);
	
	static KeycloakServerProperties keycloakServerProperties;

	@Override
	public <R> R getContextData(Class<R> type) {
		if (ServletContext.class.equals(type)) {
			return (R) augmentToRedirectContextPath((ServletContext) ResteasyProviderFactory.getContextData(type));
		} else {
			return ResteasyProviderFactory.getContextData(type);
		}
	}

	@Override
	public void pushDefaultContextObject(Class type, Object instance) {
		ResteasyProviderFactory.getContextData(Dispatcher.class).getDefaultContextObjects().put(type, instance);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void pushContext(Class type, Object instance) {
		ResteasyProviderFactory.pushContext(type, instance);
	}

	@Override
	public void clearContextData() {
		ResteasyProviderFactory.clearContextData();
	}

    private static ServletContext augmentToRedirectContextPath(ServletContext servletContext) {

        ClassLoader classLoader = servletContext.getClassLoader();
        Class[] interfaces = {ServletContext.class};

        InvocationHandler invocationHandler = (proxy, method, args) -> {

            if ("getContextPath".equals(method.getName())) {
                return keycloakServerProperties.getContextPath();
            }

            if ("getInitParameter".equals(method.getName()) && args.length == 1 && "keycloak.embedded".equals(args[0])) {
                return "true";
            }

            LOG.info("Invoke on ServletContext: method=[{}] args=[{}]", method.getName(), Arrays.toString(args));

            return method.invoke(servletContext, args);
        };

        return ServletContext.class.cast(Proxy.newProxyInstance(classLoader, interfaces, invocationHandler));
    }
}
