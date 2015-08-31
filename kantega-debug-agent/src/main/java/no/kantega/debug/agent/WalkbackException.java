package no.kantega.debug.agent;

import java.io.IOException;

public class WalkbackException extends RuntimeException {

	public WalkbackException(String message, IOException e) {
		super(message, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3122595736229802682L;
	

}
