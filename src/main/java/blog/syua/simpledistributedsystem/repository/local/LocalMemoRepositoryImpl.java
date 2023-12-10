package blog.syua.simpledistributedsystem.repository.local;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;
import blog.syua.simpledistributedsystem.repository.utils.BackupUtils;
import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;
import blog.syua.simpledistributedsystem.repository.lock.IdLock;
import blog.syua.simpledistributedsystem.repository.utils.HttpUtils;
import blog.syua.simpledistributedsystem.storage.MemoStorage;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@RequiredArgsConstructor
public class LocalMemoRepositoryImpl implements LocalMemoRepository {

	private final MemoStorage memoStorage;
	private final PrimaryStorage primaryStorage;
	private final IdLock idLock;

	private List<String> replicaURLs;
	private ExecutorService backupTread;
	private String idPrimaryURL;
	private boolean idPrimary;

	@PostConstruct
	public void init() {
		ServerConfigStorage configStorage = ServerConfigStorage.getInstance();
		List<String> replicaInfos = configStorage.getAllReplicaInfos();
		replicaInfos.remove(configStorage.getReplicaIndex());
		replicaURLs = replicaInfos.stream()
			.map(replicaInfo -> "http://" + replicaInfo + "/backup")
			.collect(Collectors.toList());
		BackupUtils.initExecutorService(replicaURLs.isEmpty() ? 1 : replicaURLs.size());
		idPrimaryURL = "http://" + configStorage.getPrimaryInfo() + "/id";
		backupTread = Executors.newSingleThreadExecutor();
		idPrimary = configStorage.amIPrimary();
	}

	@Override
	@GetMapping("/id")
	public ResponseEntity<Integer> sendNextId() {
		if (idPrimary) {
			return ResponseEntity.ok(memoStorage.getNewMemoId());
		}
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(URI.create(idPrimaryURL));
		return ResponseEntity.status(HttpStatus.SEE_OTHER)
			.headers(httpHeaders)
			.build();
	}

	@Override
	@GetMapping("/primary/{id}")
	public ResponseEntity<Void> sendPrimary(HttpServletRequest request, @PathVariable int id) {
		ResponseEntity<Void> response;
		try {
			idLock.lock(id);
			if (primaryStorage.hasPrimary(id)) {
				primaryStorage.releasePrimary(id, request.getRemoteAddr() + ":" + request.getHeader(PrimaryStorage.REPLICA_PORT_HEADER));
				response = ResponseEntity.ok().build();
			} else {
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.setLocation(URI.create(primaryStorage.getPrimaryInfo(id)));
				response = ResponseEntity.status(HttpStatus.SEE_OTHER)
					.headers(httpHeaders)
					.build();
			}
			log.info("REPLICA [REPLY] Move item to new primary");
			return response;
		} finally {
			idLock.release(id);
		}
	}

	@Override
	@PostMapping("/backup")
	public ResponseEntity<Void> backupPost(HttpServletRequest request, @RequestBody BodyMemo requestMemo) {
		return doBackupTransaction(requestMemo.getId(), requestMemo, memoStorage::save, request);
	}

	@Override
	@PutMapping("/backup/{id}")
	public ResponseEntity<Void> backupPut(HttpServletRequest request, @PathVariable int id, @RequestBody BodyMemo requestMemo) {
		return doBackupTransaction(id, requestMemo, memoStorage::put, request);
	}

	@Override
	@PatchMapping("/backup/{id}")
	public ResponseEntity<Void> backupPatch(HttpServletRequest request, @PathVariable int id, @RequestBody BodyMemo requestMemo) {
		return doBackupTransaction(id, requestMemo, memoStorage::patch, request);
	}

	@DeleteMapping("/backup/{id}")
	@Override
	public ResponseEntity<Void> backupDelete(HttpServletRequest request, @PathVariable int id) {
		return doDeleteBackupTransaction(id, memoStorage::delete, request);
	}

	@Override
	public List<Memo> findAll() {
		return memoStorage.findAll();
	}

	@Override
	public Memo find(int id) {
		return memoStorage.find(id);
	}

	@Override
	public Memo save(BodyMemo requestMemo) throws IOException {
		Integer nextId = getNextId();
		if (nextId == null) {
			return null;
		}
		return doTransaction(RequestMethod.POST.name(), nextId, requestMemo,
			(identifier, memo) -> memoStorage.save(identifier, requestMemo));
	}

	@Override
	public Memo put(int id, BodyMemo requestMemo) throws IOException {
		return doTransaction(RequestMethod.PUT.name(), id, requestMemo,
			(identifier, memo) -> memoStorage.put(id, requestMemo));
	}

	@Override
	public Memo patch(int id, BodyMemo requestMemo) throws IOException {
		return doTransaction(RequestMethod.PATCH.name(), id, requestMemo,
			(identifier, memo) -> memoStorage.patch(id, requestMemo));
	}

	@Override
	public Memo delete(int id) throws IOException {
		return doDeleteTransaction(id);
	}

	@Nullable
	private Integer getNextId() throws IOException {
		Request request = new Request.Builder()
			.url(idPrimaryURL)
			.get()
			.build();
		int nextId;
		try (Response response = HttpUtils.requestSync(request)) {
			if (!response.isSuccessful()) {
				return null;
			}
			nextId = Integer.parseInt(Objects.requireNonNull(response.body()).string());
		}
		return nextId;
	}

	@NotNull
	private ResponseEntity<Void> doBackupTransaction(int id, BodyMemo requestMemo,
		BiConsumer<Integer, BodyMemo> consumer, HttpServletRequest request) {
		try {
			idLock.lock(id);
			consumer.accept(id, requestMemo);
			changeMemoPrimary(id, request);
			log.info("REPLICA [REPLY] Acknowledge update");
			return ResponseEntity.ok().build();
		} finally {
			idLock.release(id);
		}
	}

	@NotNull
	private ResponseEntity<Void> doDeleteBackupTransaction(int id, IntConsumer consumer, HttpServletRequest request) {
		try {
			idLock.lock(id);
			consumer.accept(id);
			changeMemoPrimary(id, request);
			log.info("REPLICA [REPLY] Acknowledge update");
			return ResponseEntity.ok().build();
		} finally {
			idLock.release(id);
		}
	}

	private void changeMemoPrimary(int id, HttpServletRequest request) {
		primaryStorage.changePrimary(id, request.getRemoteAddr() + ":" + request.getHeader(PrimaryStorage.REPLICA_PORT_HEADER));
	}

	private Memo doTransaction(String method, int id, Memo memo, BiFunction<Integer, Memo, Memo> function) throws
		IOException {
		try {
			idLock.lock(id);
			if (!primaryStorage.getPrimary(id)) {
				return null;
			}
			Memo resultMemo = function.apply(id, memo);
			if (Objects.isNull(resultMemo)) {
				return null;
			}
			doBackup(method, resultMemo);
			return resultMemo;
		} finally {
			idLock.release(id);
		}
	}

	private Memo doDeleteTransaction(int id) throws IOException {
		try {
			idLock.lock(id);
			if (!primaryStorage.getPrimary(id)) {
				return null;
			}
			Memo deletedMemo = memoStorage.delete(id);
			if (Objects.isNull(deletedMemo)) {
				return null;
			}
			doBackup(RequestMethod.DELETE.name(), deletedMemo);
			return deletedMemo;
		} finally {
			idLock.release(id);
		}
	}

	public void doBackup(String method, Memo memo) {
		backupTread.execute(() -> {
			try {
				idLock.lock(memo.getId());
				if (method.equals(RequestMethod.DELETE.name())) {
					BackupUtils.syncDelete(replicaURLs, memoStorage, memo);
				} else {
					BackupUtils.syncCreateOrUpdate(replicaURLs, memoStorage, method, memo);
				}
			} catch (JsonProcessingException e) {
				log.error("Fail to Sync Memo {} {}", method, memo);
			} finally {
				idLock.release(memo.getId());
			}
		});
	}

}
