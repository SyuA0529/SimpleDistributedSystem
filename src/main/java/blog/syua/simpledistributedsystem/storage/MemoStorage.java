package blog.syua.simpledistributedsystem.storage;

import java.util.List;

import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;

public interface MemoStorage {

	int getNewMemoId();

	List<Memo> findAll();

	Memo find(int id);

	Memo save(int id, BodyMemo requestMemo);

	Memo put(int id, BodyMemo requestMemo);

	Memo patch(int id, BodyMemo requestMemo);

	Memo delete(int id);

}
