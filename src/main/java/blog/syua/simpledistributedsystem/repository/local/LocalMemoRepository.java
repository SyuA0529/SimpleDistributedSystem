package blog.syua.simpledistributedsystem.repository.local;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import blog.syua.simpledistributedsystem.repository.MemoRepository;
import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import jakarta.servlet.http.HttpServletRequest;

public interface LocalMemoRepository extends MemoRepository {

	ResponseEntity<Integer> sendNextId();

	ResponseEntity<Void> sendPrimary(HttpServletRequest request, @PathVariable int id);

	ResponseEntity<Void> backupPost(@RequestBody BodyMemo requestMemo);

	ResponseEntity<Void> backupPut(@PathVariable int id, @RequestBody BodyMemo requestMemo);

	ResponseEntity<Void> backupPatch(@PathVariable int id, @RequestBody BodyMemo requestMemo);

	ResponseEntity<Void> backupDelete(@PathVariable int id);

}
