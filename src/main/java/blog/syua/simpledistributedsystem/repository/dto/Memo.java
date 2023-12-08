package blog.syua.simpledistributedsystem.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Memo {

	private int id;
	private String title;

}
