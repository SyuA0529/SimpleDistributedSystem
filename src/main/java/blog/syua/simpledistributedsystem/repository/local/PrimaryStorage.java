package blog.syua.simpledistributedsystem.repository.local;

import java.io.IOException;

public interface PrimaryStorage {

	String REPLICA_PORT_HEADER = "replicaPort";

	boolean getPrimary(int id) throws IOException;

	String getPrimaryInfo(int id);

	boolean hasPrimary(int id);

	void releasePrimary(int id, String remoteAddr);

	void changePrimary(int id, String newPrimaryAddr);
}
