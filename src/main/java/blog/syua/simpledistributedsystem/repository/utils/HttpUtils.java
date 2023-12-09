package blog.syua.simpledistributedsystem.repository.utils;

import static blog.syua.simpledistributedsystem.repository.local.PrimaryStorage.*;

import java.io.IOException;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtils {

	private static final OkHttpClient okHttpClient = new OkHttpClient();

	private HttpUtils() {
	}

	public static Response requestSync(Request request) throws IOException {
		request = request.newBuilder()
			.header(REPLICA_PORT_HEADER, String.valueOf(ServerConfigStorage.getInstance().getPort()))
			.build();
		return okHttpClient.newCall(request)
			.execute();
	}

}
