package blog.syua.simpledistributedsystem.repository.utils;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtils {

	private static final OkHttpClient okHttpClient = new OkHttpClient();

	private HttpUtils() {
	}

	public static Response requestSync(Request request) throws IOException {
		return okHttpClient.newCall(request)
			.execute();
	}

}
