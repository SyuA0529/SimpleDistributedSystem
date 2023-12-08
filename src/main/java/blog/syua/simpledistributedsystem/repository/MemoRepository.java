package blog.syua.simpledistributedsystem.repository;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;

@RestController
public interface MemoRepository {

	okhttp3.MediaType JSON_TYPE = okhttp3.MediaType.get("application/json; charset=utf-8");
	int ERROR_CODE = -1;

	List<Memo> findAll();

	Memo find(int id);

	Memo save(BodyMemo requestMemo) throws IOException, ReflectiveOperationException;

	Memo put(int id, BodyMemo requestMemo) throws IOException, ReflectiveOperationException;

	Memo patch(int id, BodyMemo requestMemo) throws IOException, ReflectiveOperationException;

	Memo delete(int id) throws IOException;

}
