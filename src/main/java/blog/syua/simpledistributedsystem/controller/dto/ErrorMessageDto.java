package blog.syua.simpledistributedsystem.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ErrorMessageDto {

	private final String msg = "wrong URI (or body)";
	private String method;
	private String uri;
	private String body;

	public ErrorMessageDto(String method, String uri, String body) {
		this.method = method;
		this.uri = uri;
		this.body = body;
	}

}
