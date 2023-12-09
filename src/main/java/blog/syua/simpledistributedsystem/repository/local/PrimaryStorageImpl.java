package blog.syua.simpledistributedsystem.repository.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;
import blog.syua.simpledistributedsystem.repository.utils.HttpUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Component
public class PrimaryStorageImpl implements PrimaryStorage {

	private static final int MY_REPLICA_INDEX = -1;

	private final HashMap<Integer, Integer> primarys = new HashMap<>();
	private final ServerConfigStorage configStorage = ServerConfigStorage.getInstance();
	private List<String> replicaURLs;
	private int defaultPrimaryIndex;

	@PostConstruct
	public void init() {
		List<String> allReplicaInfo = configStorage.getAllReplicaInfos();
		allReplicaInfo.remove(configStorage.getReplicaIndex());
		replicaURLs = allReplicaInfo.stream()
			.map(replicaInfo -> "http://" + replicaInfo)
			.collect(Collectors.toList());
		defaultPrimaryIndex = MY_REPLICA_INDEX;
		for (int index = 0; index < allReplicaInfo.size(); index++) {
			if (allReplicaInfo.get(index).contains(configStorage.getPrimaryInfo())) {
				defaultPrimaryIndex = index;
				break;
			}
		}
	}

	@Override
	public boolean getPrimary(int id) throws IOException {
		Integer replicaIndex = primarys.getOrDefault(id, defaultPrimaryIndex);
		if (replicaIndex == MY_REPLICA_INDEX) {
			return true;
		}

		Request request = new Request.Builder()
			.url(replicaURLs.get(replicaIndex) + "/primary/" + id)
			.get()
			.build();
		log.info("REPLICA [REQUEST] Move item to new primary");
		try (Response response = HttpUtils.requestSync(request)) {
			if (response.isSuccessful()) {
				primarys.put(id, MY_REPLICA_INDEX);
				return true;
			}
			return false;
		}
	}

	@Override
	public String getPrimaryInfo(int id) {
		return replicaURLs.get(primarys.getOrDefault(id, defaultPrimaryIndex)) + "/primary/" + id;
	}

	@Override
	public boolean hasPrimary(int id) {
		return primarys.getOrDefault(id, defaultPrimaryIndex) == MY_REPLICA_INDEX;
	}

	@Override
	public void releasePrimary(int id, String newPrimaryAddr) {
		if (primarys.getOrDefault(id, defaultPrimaryIndex) != MY_REPLICA_INDEX) {
			throw new IllegalArgumentException("primary가 아닙니다");
		}
		String findReplicaInfo = replicaURLs.stream()
			.filter(replicaInfo -> replicaInfo.contains(newPrimaryAddr))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("등록되지 않은 Replica 입니다"));
		primarys.put(id, replicaURLs.indexOf(findReplicaInfo));
	}

}
