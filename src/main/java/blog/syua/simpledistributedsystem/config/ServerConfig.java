package blog.syua.simpledistributedsystem.config;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServerConfig {

	private String servicePort;
	private String sync;
	private List<String> replicas;

}
