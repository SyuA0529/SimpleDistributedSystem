package blog.syua.simpledistributedsystem.repository.remote;

import java.io.IOException;
import java.util.concurrent.Callable;

import blog.syua.simpledistributedsystem.repository.utils.HttpUtils;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackupTask implements Callable<Boolean> {

	private final Request request;

	public BackupTask(String replicaURL, String method, RequestBody body) {
		this.request = new Request.Builder()
			.url(replicaURL)
			.method(method, body)
			.build();
	}

	@Override
	public Boolean call() {
		// W3
		try (Response response = HttpUtils.requestSync(request)) {
			// W4
			return response.isSuccessful();
		} catch (IOException e) {
			return false;
		}
	}

}
