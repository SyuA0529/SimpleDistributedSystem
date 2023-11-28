package blog.syua.simpledistributedsystem.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class DistributedSystemWebServerFactory implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

	@Override
	public void customize(ConfigurableWebServerFactory factory) {
		factory.setPort(ServerConfigStorage.getInstance().getPort());
	}

}
