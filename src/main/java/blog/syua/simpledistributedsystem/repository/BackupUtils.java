package blog.syua.simpledistributedsystem.repository;

import static blog.syua.simpledistributedsystem.repository.MemoRepository.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;
import blog.syua.simpledistributedsystem.repository.remote.BackupTask;
import blog.syua.simpledistributedsystem.storage.MemoStorage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackupUtils {

	private static final byte[] EMPTY_BYTES = new byte[0];
	private static ExecutorService executorService;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private BackupUtils() {
	}

	public static void initExecutorService(int size) {
		executorService = Executors.newFixedThreadPool(size);
	}

	public static boolean syncCreateOrUpdate(List<String> replicaURLs, MemoStorage memoStorage, String method,
		Memo savedMemo) throws JsonProcessingException {
		okhttp3.RequestBody updateRequestBody = okhttp3.RequestBody.create(objectMapper.writeValueAsBytes(savedMemo),
			JSON_TYPE);
		String additionalUrl = method.equals(RequestMethod.POST.name()) ? "" : "/" + savedMemo.getId();
		// W3 & W4
		if (!tellUpdate(replicaURLs, additionalUrl, method, updateRequestBody)) {
			memoStorage.delete(savedMemo.getId());
			okhttp3.RequestBody deleteRequestBody = okhttp3.RequestBody.create(EMPTY_BYTES);
			tellUpdate(replicaURLs, "/" + savedMemo.getId(), RequestMethod.DELETE.name(), deleteRequestBody);
			return false;
		}
		return true;
	}

	public static boolean syncDelete(List<String> replicaURLs, MemoStorage memoStorage, Memo memo) throws
		JsonProcessingException {
		okhttp3.RequestBody deleteRequestBody = okhttp3.RequestBody.create(EMPTY_BYTES);
		// W3 & W4
		if (!tellUpdate(replicaURLs, "/" + memo.getId(), RequestMethod.DELETE.name(), deleteRequestBody)) {
			BodyMemo savedMemo = new BodyMemo(memo.getId(), memo.getTitle(),
				memo instanceof BodyMemo ? ((BodyMemo)memo).getBody() : "");
			memoStorage.save(memo.getId(), savedMemo);
			okhttp3.RequestBody postRequestBody = okhttp3.RequestBody.create(objectMapper.writeValueAsBytes(memo),
				JSON_TYPE);
			tellUpdate(replicaURLs, "", RequestMethod.POST.name(), postRequestBody);
			return false;
		}
		return true;
	}

	private static boolean tellUpdate(List<String> replicaURLs, String additionalURI, String method,
		okhttp3.RequestBody requestBody) {
		List<BackupTask> backupTasks = replicaURLs.stream()
			.map(replicaURL -> new BackupTask(replicaURL + additionalURI, method, requestBody))
			.toList();
		log.info("REPLICA [REQUEST] Tell backups to update to {}", replicaURLs);
		boolean isSuccessful = true;
		try {
			for (Future<Boolean> future : executorService.invokeAll(backupTasks)) {
				isSuccessful &= future.get();
			}
		} catch (Exception exception) {
			log.error(exception.getMessage());
			return false;
		}
		return isSuccessful;
	}

}
