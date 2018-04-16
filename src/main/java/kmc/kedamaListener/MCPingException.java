package kmc.kedamaListener;

public class MCPingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1813419205366351662L;

	public MCPingException() {
		
	}

	public MCPingException(String message) {
		super(message);
	}

	public MCPingException(Throwable cause) {
		super(cause);
	}

	public MCPingException(String message, Throwable cause) {
		super(message, cause);
	}

	public MCPingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
