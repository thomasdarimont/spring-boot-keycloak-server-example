/**
 * 
 */
package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.util.ResteasyProvider;

/**
 * @author mike
 */
public class BootRestEasyProvider implements ResteasyProvider {

	@Override
	public <R> R getContextData(Class<R> type) {
		return ResteasyProviderFactory.getContextData(type);
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

}
