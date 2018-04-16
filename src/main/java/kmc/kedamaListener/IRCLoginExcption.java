package kmc.kedamaListener;

public class IRCLoginExcption extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 847622747542674267L;

	public IRCLoginExcption() {
		super();
	}

	public IRCLoginExcption(String message) {
		super(message);
	}

	public IRCLoginExcption(Throwable cause) {
		super(cause);
	}

	public IRCLoginExcption(String message, Throwable cause) {
		super(message, cause);
	}

	public IRCLoginExcption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
