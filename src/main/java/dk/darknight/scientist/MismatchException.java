package dk.darknight.scientist;

import lombok.Getter;

@Getter
public class MismatchException extends RuntimeException {
	private static final long serialVersionUID = 6571064759698006300L;
	private String name;
	private Result<?, ?> result;

	public <T, TClean> MismatchException(String name, Result<T, TClean> result) {
		super("Experiment '" + name + "' observations mismatched");
		this.name = name;
		this.result = result;
	}

}