package blog.syua.simpledistributedsystem.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import blog.syua.simpledistributedsystem.controller.dto.DeleteSuccessDto;
import blog.syua.simpledistributedsystem.repository.MemoRepository;
import blog.syua.simpledistributedsystem.repository.dto.BodyMemo;
import blog.syua.simpledistributedsystem.repository.dto.Memo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class MemoController {

	private final MemoRepository memoRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping
	public List<Memo> getAllMemos() {
		return memoRepository.findAll();
	}

	@GetMapping("/{id}")
	public Memo getMemo(@PathVariable int id) {
		return memoRepository.find(id);
	}

	@PostMapping
	public Memo saveMemo(HttpServletRequest request, @RequestBody BodyMemo requestMemo) throws
		IOException,
		ReflectiveOperationException {
		logWriteRequest(request, requestMemo);
		Memo savedMemo = memoRepository.save(requestMemo);
		return returnResult(request.getMethod(), request.getRequestURI(), savedMemo);
	}

	@PutMapping("/{id}")
	public Memo putMemo(HttpServletRequest request, @PathVariable int id, @RequestBody BodyMemo requestMemo) throws
		IOException,
		ReflectiveOperationException {
		logWriteRequest(request, requestMemo);
		Memo savedMemo = memoRepository.put(id, requestMemo);
		return returnResult(request.getMethod(), request.getRequestURI(), savedMemo);
	}

	@PatchMapping("/{id}")
	public Memo patchMemo(HttpServletRequest request, @PathVariable int id, @RequestBody BodyMemo requestMemo) throws
		IOException,
		ReflectiveOperationException {
		logWriteRequest(request, requestMemo);
		Memo savedMemo = memoRepository.patch(id, requestMemo);
		return returnResult(request.getMethod(), request.getRequestURI(), savedMemo);
	}

	@DeleteMapping("/{id}")
	public DeleteSuccessDto deleteMemo(@PathVariable int id) throws
		IOException {
		log.info("CLIENT [REQUEST] DELETE /note/{} ", id);
		if (Objects.isNull(memoRepository.delete(id))) {
			throw new IllegalArgumentException();
		} else {
			DeleteSuccessDto deleteSuccessDto = new DeleteSuccessDto();
			log.info("CLIENT [REPLY] DELETE /note{} {{}} ", id, new String(objectMapper.writeValueAsBytes(deleteSuccessDto), StandardCharsets.UTF_8));
			return deleteSuccessDto;
		}
	}

	@NotNull
	private Memo returnResult(String method, String uri, Memo savedMemo) throws JsonProcessingException {
		if (Objects.isNull(savedMemo)) {
			throw new IllegalArgumentException();
		}
		log.info("CLIENT [REPLY] {} {} {{}} ", method, uri, new String(objectMapper.writeValueAsBytes(savedMemo), StandardCharsets.UTF_8));
		return savedMemo;
	}

	private void logWriteRequest(HttpServletRequest request, Object jsonObject) throws JsonProcessingException {
		log.info("CLIENT [REQUEST] {} {} {{}} ",
			request.getMethod(), request.getRequestURI(), new String(objectMapper.writeValueAsBytes(jsonObject), StandardCharsets.UTF_8));

	}

}
