package blog.syua.simpledistributedsystem.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BodyMemo extends Memo {

	private String body;

	public BodyMemo(int id, String title, String body) {
		super(id, title);
		this.body = body;
	}

	public BodyMemo(int id, BodyMemo otherMemo) {
		super(id, otherMemo.getTitle());
		this.body = otherMemo.getBody();
	}

}
