package blog.syua.simpledistributedsystem.repository.remote;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;
import blog.syua.simpledistributedsystem.repository.BackupUtils;
import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;
import blog.syua.simpledistributedsystem.repository.lock.IdLock;
import blog.syua.simpledistributedsystem.storage.MemoStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/primary")
@RequiredArgsConstructor
public class RemotePrimaryMemoRepository implements RemoteMemoRepository {

	private final MemoStorage memoStorage;
	private final IdLock idLock;
	private List<String> replicaURLs;

	@PostConstruct
	public void init() {
		replicaURLs = ServerConfigStorage.getInstance().getReplicaInfos().stream()
			.map(replicaInfo -> "http://" + replicaInfo + "/backup")
			.collect(Collectors.toList());
		BackupUtils.initExecutorService(replicaURLs.isEmpty() ? 1 : replicaURLs.size());
	}

	@Override
	@GetMapping
	public List<Memo> findAll() {
		return memoStorage.findAll();
	}

	@Override
	public Memo find(int id) {
		return memoStorage.find(id);
	}

	@Override
	@PostMapping
	public Memo save(@RequestBody BodyMemo requestMemo) throws JsonProcessingException {
		int memoId = memoStorage.getNewMemoId();
		return doCreateOrUpdateTransaction(RequestMethod.POST.name(), memoId, requestMemo,
			memoStorage::save);
	}

	@Override
	@PutMapping("/{id}")
	public Memo put(@PathVariable int id, @RequestBody BodyMemo requestMemo) throws JsonProcessingException {
		return doCreateOrUpdateTransaction(RequestMethod.PUT.name(), id, requestMemo,
			memoStorage::put);
	}

	@Override
	@PatchMapping("/{id}")
	public Memo patch(@PathVariable int id, @RequestBody BodyMemo requestMemo) throws JsonProcessingException {
		return doCreateOrUpdateTransaction(RequestMethod.PATCH.name(), id, requestMemo,
			memoStorage::patch);
	}

	@Override
	@DeleteMapping("/{id}")
	public Memo delete(@PathVariable int id) throws IOException {
		return doDeleteTransaction(id);
	}

	private boolean backup(Memo memo, String method) throws
		JsonProcessingException {
		return method.equals(RequestMethod.DELETE.name()) ?
			BackupUtils.syncDelete(replicaURLs, memoStorage, memo) :
			BackupUtils.syncCreateOrUpdate(replicaURLs, memoStorage, method, memo);
	}

	private Memo doCreateOrUpdateTransaction(String method, int id, BodyMemo memo,
		BiFunction<Integer, BodyMemo, Memo> function) throws JsonProcessingException {
		try {
			idLock.lock(id);
			Memo savedMemo = function.apply(id, memo);
			if (Objects.isNull(savedMemo) || !backup(savedMemo, method)) {
				return null;
			}
			return savedMemo;
		} finally {
			idLock.release(id);
			log.info("REPLICA [REPLY] Forward request to primary");
		}
	}

	@Nullable
	private Memo doDeleteTransaction(int id) throws JsonProcessingException {
		boolean successful = false;
		try {
			idLock.lock(id);
			Memo deletedMemo = memoStorage.delete(id);
			if (Objects.isNull(deletedMemo) || !backup(deletedMemo, RequestMethod.DELETE.name())) {
				return null;
			}
			successful = true;
			return deletedMemo;
		} finally {
			idLock.release(id);
			if (successful) {
				idLock.remove(id);
			}
			log.info("REPLICA [REPLY] Forward request to primary");
		}
	}

}
