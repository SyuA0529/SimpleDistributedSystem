package blog.syua.simpledistributedsystem;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import blog.syua.simpledistributedsystem.config.ServerConfigStorage;

@SpringBootApplication
public class SimpleDistributedSystemApplication {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("Wrong Commandline Argument");
		}
		loadConfig(new File(args[0]));
		SpringApplication.run(SimpleDistributedSystemApplication.class, args);
	}

	private static void loadConfig(File file) {
		if (!(file.isFile() && file.canRead())) {
			throw new IllegalArgumentException("Cannot Find Config File");
		}
		try {
			ServerConfigStorage.getInstance()
				.loadConfig(file);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot Load Config from " + file.getName());
		}
	}

}
