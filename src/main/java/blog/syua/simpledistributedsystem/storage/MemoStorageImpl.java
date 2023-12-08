package blog.syua.simpledistributedsystem.storage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.EmptyBodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;

@Component
public class MemoStorageImpl implements MemoStorage {

	private final Map<Integer, Memo> memos = new HashMap<>();

	private int nextId = 1;

	@Override
	public synchronized int getNewMemoId() {
		return nextId++;
	}

	@Override
	public List<Memo> findAll() {
		return memos.values().stream()
			.sorted(Comparator.comparingInt(Memo::getId))
			.collect(Collectors.toList());
	}

	@Override
	public Memo find(int id) {
		return memos.get(id);
	}

	@Override
	public Memo save(int id, BodyMemo requestMemo) {
		if (Objects.nonNull(memos.get(id))) {
			throw new IllegalArgumentException("이미 존재하는 메모입니다");
		}
		return saveNewMemo(id, requestMemo);
	}

	@Override
	public Memo put(int id, BodyMemo requestMemo) {
		if (!memos.containsKey(id)) {
			return null;
		}
		return saveNewMemo(id, requestMemo);
	}

	@Override
	public Memo patch(int id, BodyMemo requestMemo) {
		if (!memos.containsKey(id)) {
			return null;
		}
		return saveNewMemo(id, updateMemo(id, requestMemo));
	}

	@Override
	public Memo delete(int id) {
		return memos.remove(id);
	}

	@NotNull
	private BodyMemo updateMemo(int id, BodyMemo requestMemo) {
		Memo savedMemo = memos.get(id);
		String title = requestMemo.getTitle();
		if (Objects.isNull(title) || title.isEmpty()) {
			title = savedMemo.getTitle();
		}
		String body = requestMemo.getBody();
		if (Objects.isNull(body) || body.isEmpty()) {
			body = savedMemo instanceof BodyMemo ? ((BodyMemo)savedMemo).getBody() : "";
		}
		return new BodyMemo(id, title, body);
	}

	private Memo saveNewMemo(int id, BodyMemo requestMemo) {
		Memo memo;
		String body = requestMemo.getBody();
		if (Objects.isNull(body) || body.isEmpty()) {
			memo = new EmptyBodyMemo(id, requestMemo.getTitle());
		} else {
			memo = new BodyMemo(id, requestMemo);
		}
		memos.put(id, memo);
		return memo;
	}

}
