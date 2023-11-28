package blog.syua.simpledistributedsystem.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

public class ServerConfigStorage {

	@Getter
	private static final ServerConfigStorage instance = new ServerConfigStorage();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ServerConfig serverConfig = null;

	public synchronized void loadConfig(File configFile) throws IOException {
		serverConfig = objectMapper.readValue(configFile, ServerConfig.class);
	}

	public int getPort() {
		return Integer.parseInt(serverConfig.getServerPort());
	}

}
