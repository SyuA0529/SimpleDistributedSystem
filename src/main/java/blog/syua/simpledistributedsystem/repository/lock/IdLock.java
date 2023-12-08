package blog.syua.simpledistributedsystem.repository.lock;

public interface IdLock {

	void lock(int id);

	void release(int id);

	void remove(int id);

}
