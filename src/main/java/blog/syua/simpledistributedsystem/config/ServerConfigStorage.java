package blog.syua.simpledistributedsystem.config;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

public class ServerConfigStorage {

	@Getter
	private static final ServerConfigStorage instance = new ServerConfigStorage();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ServerConfig serverConfig = null;

	@Getter
	private int replicaIndex;

	public synchronized void loadConfig(File configFile) throws IOException {
		serverConfig = objectMapper.readValue(configFile, ServerConfig.class);
		serverConfig.getReplicas()
			.forEach(ServerConfigStorage::verifyStorageInfo);
		setReplicaIndex();
	}

	public boolean amIPrimary() {
		return replicaIndex == 0;
	}

	public int getPort() {
		return Integer.parseInt(serverConfig.getServicePort());
	}

	public String getSyncType() {
		return serverConfig.getSync();
	}

	public String getPrimaryInfo() {
		List<String> replicas = serverConfig.getReplicas();
		verifyReplicaInfos(replicas);
		return replicas.get(0);
	}

	public List<String> getReplicaInfos() {
		List<String> replicaInfos = getAllReplicaInfos();
		replicaInfos.remove(replicaIndex);
		if (!amIPrimary()) {
			replicaInfos.remove(0);
		}
		return replicaInfos;
	}

	public List<String> getAllReplicaInfos() {
		List<String> replicas = new ArrayList<>(serverConfig.getReplicas());
		verifyReplicaInfos(replicas);
		return replicas;
	}

	private static void verifyReplicaInfos(List<String> replicas) {
		if (Objects.isNull(replicas) || replicas.isEmpty()) {
			throw new IllegalStateException("Replica가 존재하지 않습니다");
		}
	}

	private static void verifyStorageInfo(String info) {
		if (info.split(":").length != 2) {
			throw new IllegalStateException("Storage의 주소 및 포트 값이 올바르지 않습니다");
		}
	}

	private void setReplicaIndex() throws UnknownHostException {
		List<String> replicaInfos = getAllReplicaInfos();
		for (int index = 0; index < replicaInfos.size(); index++) {
			String replicaInfo = replicaInfos.get(index);
			verifyStorageInfo(replicaInfo);
			String[] infos = replicaInfo.split(":");
			InetAddress replicaAddr = InetAddress.getByName(infos[0]);
			if (getPort() == Integer.parseInt(infos[1]) &&
				(replicaAddr.isLoopbackAddress() || replicaAddr.isAnyLocalAddress())) {
				replicaIndex = index;
				break;
			}
		}
	}

}
