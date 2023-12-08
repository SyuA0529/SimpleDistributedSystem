package blog.syua.simpledistributedsystem.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import blog.syua.simpledistributedsystem.repository.MemoRepository;
import blog.syua.simpledistributedsystem.repository.local.LocalMemoRepositoryImpl;
import blog.syua.simpledistributedsystem.repository.local.PrimaryStorage;
import blog.syua.simpledistributedsystem.repository.lock.IdLock;
import blog.syua.simpledistributedsystem.repository.remote.RemoteMemoRepository;
import blog.syua.simpledistributedsystem.repository.remote.RemotePrimaryMemoRepository;
import blog.syua.simpledistributedsystem.repository.remote.RemoteReplicaMemoRepository;
import blog.syua.simpledistributedsystem.storage.MemoStorage;
import jakarta.validation.constraints.NotNull;

@Configuration
public class BeanConfig {

	private static final String REMOTE_WRITE = "remote-write";
	private static final String LOCAL_WRITE = "local-write";

	@Bean
	public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
		return factory -> factory.setPort(ServerConfigStorage.getInstance().getPort());
	}

	@Bean
	public MemoRepository memoRepository(MemoStorage memoStorage, PrimaryStorage primaryStorage,
		IdLock idLock) {
		ServerConfigStorage configStorage = ServerConfigStorage.getInstance();
		String syncType = configStorage.getSyncType();
		if (syncType.equals(REMOTE_WRITE)) {
			return getRemoteMemoRepository(configStorage, memoStorage, idLock);
		}
		if (syncType.equals(LOCAL_WRITE)) {
			return new LocalMemoRepositoryImpl(memoStorage, primaryStorage, idLock);
		}
		throw new IllegalStateException("도달할 수 없는 위치입니다");
	}

	@NotNull
	private static RemoteMemoRepository getRemoteMemoRepository(ServerConfigStorage configStorage,
		MemoStorage memoStorage, IdLock idLock) {
		if (configStorage.amIPrimary()) {
			return new RemotePrimaryMemoRepository(memoStorage, idLock);
		}
		return new RemoteReplicaMemoRepository(memoStorage);
	}

}
