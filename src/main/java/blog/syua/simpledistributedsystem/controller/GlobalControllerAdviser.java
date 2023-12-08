package blog.syua.simpledistributedsystem.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import blog.syua.simpledistributedsystem.controller.dto.ErrorMessageDto;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdviser {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorMessageDto> exceptionHandler(HttpServletRequest request, Exception exception) throws IOException {
		log.error(exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorMessageDto(request.getMethod(), request.getRequestURI(), getRequestBody(request)));
	}

	private static String getRequestBody(HttpServletRequest request) throws IOException {
		ServletInputStream inputStream = request.getInputStream();
		String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		inputStream.close();
		return body;
	}

}
