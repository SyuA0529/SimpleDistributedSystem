package blog.syua.simpledistributedsystem.repository.remote;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;
import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;
import blog.syua.simpledistributedsystem.storage.MemoStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@RequestMapping("/backup")
@RequiredArgsConstructor
public class RemoteReplicaMemoRepository implements RemoteMemoRepository {

	private final MemoStorage memoStorage;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private String primaryURL;
	private okhttp3.OkHttpClient httpClient;

	@PostConstruct
	public void init() throws IOException {
		String primaryInfo = ServerConfigStorage.getInstance()
			.getPrimaryInfo();
		httpClient = new OkHttpClient();
		primaryURL = "http://" + primaryInfo + "/primary";
		syncAllMemos();
	}

	@PostMapping
	public ResponseEntity<Void> backupPost(@RequestBody BodyMemo requestMemo) {
		memoStorage.save(requestMemo.getId(), requestMemo);
		log.info("REPLICA [REPLY] Acknowledge update (W4)");
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}")
	public ResponseEntity<Void> backupPut(@PathVariable int id, @RequestBody BodyMemo requestMemo) {
		memoStorage.put(id, requestMemo);
		log.info("REPLICA [REPLY] Acknowledge update (W4)");
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Void> backupPatch(@PathVariable int id, @RequestBody BodyMemo requestMemo) {
		memoStorage.patch(id, requestMemo);
		log.info("REPLICA [REPLY] Acknowledge update (W4)");
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> backupDelete(@PathVariable int id) {
		memoStorage.delete(id);
		log.info("REPLICA [REPLY] Acknowledge update (W4)");
		return ResponseEntity.ok().build();
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
		Request request = new Request.Builder()
			.url(primaryURL)
			.post(okhttp3.RequestBody.create(objectMapper.writeValueAsBytes(requestMemo), JSON_TYPE))
			.build();
		return forwardRequest(request);
	}

	@Override
	public Memo put(int id, BodyMemo requestMemo) throws IOException {
		Request request = new Request.Builder()
			.url(primaryURL + "/" + id)
			.put(okhttp3.RequestBody.create(objectMapper.writeValueAsBytes(requestMemo), JSON_TYPE))
			.build();
		return forwardRequest(request);
	}

	@Override
	public Memo patch(int id, BodyMemo requestMemo) throws IOException {
		Request request = new Request.Builder()
			.url(primaryURL + "/" + id)
			.patch(okhttp3.RequestBody.create(objectMapper.writeValueAsBytes(requestMemo), JSON_TYPE))
			.build();
		return forwardRequest(request);
	}

	@Override
	public Memo delete(int id) throws IOException {
		Request request = new Request.Builder()
			.url(primaryURL + "/" + id)
			.delete()
			.build();
		// W1
		try (Response response = httpClient.newCall(request).execute()) {
			return objectMapper.readValue(Objects.requireNonNull(response.body()).string(), BodyMemo.class);
		}
	}

	private void syncAllMemos() throws IOException {
		Request syncRequest = new Request.Builder()
			.url(primaryURL)
			.get()
			.build();
		try (Response response = httpClient.newCall(syncRequest).execute()) {
			String responseBody = Objects.requireNonNull(response.body()).string();
			isResponseSuccessful(response, responseBody);
			objectMapper.readValue(responseBody, new TypeReference<List<BodyMemo>>() {
			}).forEach(memo -> memoStorage.save(memo.getId(), memo));
		}
	}

	private static boolean isResponseSuccessful(Response response, String responseBody) {
		return response.isSuccessful() &&
			!Objects.isNull(responseBody) &&
			!responseBody.equals(String.valueOf(ERROR_CODE));
	}

	private Memo forwardRequest(Request request) throws IOException {
		// W1
		log.info("REPLICA [REQUEST] Forward request to primary (W2)");
		try (Response response = httpClient.newCall(request).execute()) {
			String responseBody = Objects.requireNonNull(response.body()).string();
			boolean isSuccess = isResponseSuccessful(response, responseBody);
			// W5
			return isSuccess ? find(objectMapper.readValue(responseBody, BodyMemo.class).getId()) : null;
		}
	}

}
