package blog.syua.simpledistributedsystem.repository.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

@Component
public class IdLockImpl implements IdLock {

	private final Map<Integer, ReentrantLock> locks = new HashMap<>();

	@Override
	public void lock(int id) {
		getLock(id).lock();
	}

	@Override
	public void release(int id) {
		getLock(id).unlock();
	}

	@Override
	public void remove(int id) {
		locks.remove(id);
	}

	private synchronized ReentrantLock getLock(int id) {
		ReentrantLock lock = locks.get(id);
		if (Objects.isNull(lock)) {
			lock = new ReentrantLock(false);
			locks.put(id, lock);
		}
		return lock;
	}

}
