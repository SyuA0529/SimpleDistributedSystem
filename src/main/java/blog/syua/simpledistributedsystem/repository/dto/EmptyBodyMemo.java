package blog.syua.simpledistributedsystem.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmptyBodyMemo extends Memo {

	public EmptyBodyMemo(int id, String title) {
		super(id, title);
	}

}
